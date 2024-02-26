package com.dominikkorsa.discordintegration

import com.dominikkorsa.discordintegration.response.NameToUUIDResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.bukkit.entity.Player
import java.util.*

class AvatarService(private val plugin: DiscordIntegration) {
    private val uuids = HashMap<String, String>()
    private val client =  HttpClient(CIO) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    private var lastCheckTooManyRequests = false

    private suspend fun updateNicknameUUID(playerName: String) {
        try {
            val response = client.get(
                "https://api.mojang.com/users/profiles/minecraft/${playerName}"
            )
            uuids[playerName] = response.body<NameToUUIDResponse>().id
            lastCheckTooManyRequests = false
        } catch (error: Exception) {
            if (error is ClientRequestException) when (error.response.status) {
                HttpStatusCode.NoContent -> return
                HttpStatusCode.NotFound -> return
                HttpStatusCode.TooManyRequests -> {
                    if (!lastCheckTooManyRequests) {
                        plugin.logger.warning("Failed to get UUID of player $playerName - Exceeded Mojang's API rate limit")
                    }
                    lastCheckTooManyRequests = true
                    return
                }
            }
            plugin.logger.warning("Failed to get UUID of player $playerName")
            plugin.logger.warning(error.message)
        }
    }

    private suspend fun getNicknameUUID(playerName: String): String? {
        if (!uuids.containsKey(playerName)) {
            updateNicknameUUID(playerName)
        }
        return uuids[playerName]
    }

    suspend fun getAvatarUrl(playerId: UUID, playerName: String): String {
        var result = plugin.configManager.chat.avatarUrl
            .replace("%player%", playerName)

        if (result.contains("%uuid")) {
            var uuid = playerId.toString()
            if (plugin.configManager.chat.avatarOfflineMode) getNicknameUUID(playerName)?.let { uuid = it }
            result = result.replace("%uuid%", uuid)
        }

        return result
    }

    suspend fun getAvatarUrl(player: Player) = getAvatarUrl(player.uniqueId, player.name)
}
