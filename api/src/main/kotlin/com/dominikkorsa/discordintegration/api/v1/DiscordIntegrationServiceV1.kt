package com.dominikkorsa.discordintegration.api.v1

import discord4j.core.`object`.entity.Message
import org.bukkit.entity.Player

@Suppress("unused")
interface DiscordIntegrationServiceV1 {
    val minorAPIVersion: Int

    suspend fun reload()
    suspend fun sendChatToDiscord(player: Player, content: String)
    suspend fun sendJoinMessageToDiscord(player: Player)
    suspend fun sendQuitMessageToDiscord(player: Player)
    suspend fun sendDeathMessageToDiscord(player: Player, deathMessage: String?)
    suspend fun sendDiscordMessageToChat(message: Message)
    val linking: Linking
}
