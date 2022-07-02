package com.dominikkorsa.discordintegration

import com.dominikkorsa.discordintegration.response.NameToUUIDResponse
import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.jvm.javaio.*
import org.bukkit.entity.Player
import java.util.*

class AvatarService(private val plugin: DiscordIntegration) {
    private val uuids = HashMap<String, String>()
    private val client = HttpClient(CIO)

    private suspend fun updateNicknameUUID(playerName: String) {
        try {
            val response: HttpResponse = client.get(
                "https://api.mojang.com/users/profiles/minecraft/${playerName}"
            )
            val reader = response.content.toInputStream().reader(Charsets.UTF_8)
            val responseData: NameToUUIDResponse? = Gson().fromJson(reader, NameToUUIDResponse::class.java)

            if (responseData != null) uuids[playerName] = responseData.id
        } catch (error: Exception) {
            error.printStackTrace()
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
        if (plugin.configManager.avatarOfflineMode) getNicknameUUID(playerName)?.let { uuid = it }
        return plugin.configManager.avatarUrl
            .replace("%player%", playerName)
            .replace("%uuid%", uuid)
    }

    suspend fun getAvatarUrl(player: Player) = getAvatarUrl(player.uniqueId, player.name)
}
