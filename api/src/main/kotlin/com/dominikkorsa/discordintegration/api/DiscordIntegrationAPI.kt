package com.dominikkorsa.discordintegration.api

import discord4j.core.`object`.entity.Message
import org.bukkit.entity.Player
import org.bukkit.event.entity.PlayerDeathEvent

interface DiscordIntegrationAPI {
    suspend fun reload()
    suspend fun sendChatToDiscord(player: Player, message: String)
    suspend fun sendJoinMessageToDiscord(player: Player)
    suspend fun sendQuitMessageToDiscord(player: Player)
    suspend fun sendDeathMessageToDiscord(event: PlayerDeathEvent)
    suspend fun sendDiscordMessageToChat(message: Message)
    val linking: LinkingAPI
}
