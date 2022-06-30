package com.dominikkorsa.discordintegration

import com.dominikkorsa.discordintegration.response.NameToUUIDResponse
import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.jvm.javaio.*
import org.bukkit.entity.Player

class AvatarService(private val plugin: DiscordIntegration) {
    private val uuids = HashMap<String, String>()
    private val client = HttpClient(CIO)

    private suspend fun updateNicknameUUID(player: Player) {
        try {
            val response: HttpResponse = client.get(
                "https://api.mojang.com/users/profiles/minecraft/${player.name}"
            )
            val reader = response.content.toInputStream().reader(Charsets.UTF_8)
            val responseData: NameToUUIDResponse? = Gson().fromJson(reader, NameToUUIDResponse::class.java)

            if (responseData != null) uuids[player.name] = responseData.id
        } catch (error: Exception) {
            error.printStackTrace()
        }
    }

    private suspend fun getNicknameUUID(player: Player): String? {
        if (!uuids.containsKey(player.name)) {
            updateNicknameUUID(player)
        }
        return uuids[player.name]
    }

    suspend fun getAvatarUrl(player: Player): String {
        var uuid = player.uniqueId.toString()
        if (plugin.configManager.avatarOfflineMode) getNicknameUUID(player)?.let { uuid = it }
        return plugin.configManager.avatarUrl
            .replace("%player%", player.name)
            .replace("%uuid%", uuid)
    }
}
