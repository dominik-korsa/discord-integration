package com.dominikkorsa.discordintegration

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import kotlin.random.Random

class ChatListener(private val plugin: DiscordIntegration) : Listener {
    @EventHandler
    suspend fun onPlayerChat(event: AsyncPlayerChatEvent) {
        val avatar = when (plugin.configManager.charRenderHead) {
            true -> "https://crafatar.com/renders/head/${event.player.uniqueId}?overlay"
            false -> "https://crafatar.com/avatars/${event.player.uniqueId}?overlay"
        }
        plugin.client.sendChatMessage(
            event.player.displayName,
            avatar,
            event.message,
        )
    }
}
