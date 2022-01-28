package com.dominikkorsa.discordintegration.listener

import com.dominikkorsa.discordintegration.DiscordIntegration
import org.bukkit.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent

class ChatListener(private val plugin: DiscordIntegration) : Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    suspend fun onPlayerChat(event: AsyncPlayerChatEvent) {
        plugin.client.sendWebhook(
            plugin.client.getPlayerWebhookBuilder(event.player)
                .content(ChatColor.stripColor(event.message).orEmpty())
                .build(),
        )
    }
}
