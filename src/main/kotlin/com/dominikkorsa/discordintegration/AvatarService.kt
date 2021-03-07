package com.dominikkorsa.discordintegration

import com.dominikkorsa.discordintegration.response.NameToUUIDResponse
import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.jvm.javaio.*
import org.bukkit.entity.Player
import java.net.URL

class AvatarService {
    private val uuids = HashMap<String, String>()
    private val client = HttpClient(CIO)

    enum class AvatarType {
        Face,
        Head,
    }

    private suspend fun updateNicknameUUID(player: Player) {
        try {
            val response: HttpResponse = client.get("https://api.mojang.com/users/profiles/minecraft/${player.name}")
            val reader = response.content.toInputStream().reader(Charsets.UTF_8)
            val responseData = Gson().fromJson(reader, NameToUUIDResponse::class.java)
            uuids[player.name] = responseData.id
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

    suspend fun getAvatarUrl(player: Player, type: AvatarType): String {
        val uuid =  getNicknameUUID(player) ?: player.uniqueId.toString()
        return when(type) {
            AvatarType.Face -> "https://crafatar.com/avatars/${uuid}?overlay"
            AvatarType.Head -> "https://crafatar.com/renders/head/${uuid}?overlay"
        }
    }
}
