package com.dominikkorsa.discordintegration

import com.dominikkorsa.discordintegration.response.NameToUUIDResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
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
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    private suspend fun updateNicknameUUID(playerName: String) {
        try {
            val response = client.get(
                "https://api.mojang.com/users/profiles/minecraft/${playerName}"
            )
            if (response.status == HttpStatusCode.NoContent) return
            uuids[playerName] = response.body<NameToUUIDResponse>().id
        } catch (error: Exception) {
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
        var uuid = playerId.toString()
        if (plugin.configManager.chat.avatarOfflineMode) getNicknameUUID(playerName)?.let { uuid = it }
        return plugin.configManager.chat.avatarUrl
            .replace("%player%", playerName)
            .replace("%uuid%", uuid)
    }

    suspend fun getAvatarUrl(player: Player) = getAvatarUrl(player.uniqueId, player.name)
}
