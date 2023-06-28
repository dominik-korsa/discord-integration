package com.dominikkorsa.discordintegration.api

import org.bukkit.entity.Player

interface DiscordIntegrationServiceV1 {
    suspend fun reload()
    suspend fun sendChatToDiscord(player: Player, message: String)
    suspend fun sendJoinMessageToDiscord(player: Player)
    suspend fun sendQuitMessageToDiscord(player: Player)
    suspend fun sendDeathMessageToDiscord(player: Player, deathMessage: String?)
//    suspend fun sendDiscordMessageToChat(message: Message)
//    val linking: LinkingAPI
}
