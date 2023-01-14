package com.dominikkorsa.discordintegration.listener

import com.dominikkorsa.discordintegration.DiscordIntegration
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.dynmap.DynmapWebChatEvent

class DynmapChatListener(private val plugin: DiscordIntegration) : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    suspend fun onDynmapChat(event: DynmapWebChatEvent) {
        plugin.client.sendWebhook(
            plugin.client.getWebhookBuilder()
                .username("[WEB] ${event.name}")
                .content(plugin.discordFormatter.formatMessageContent(event.message))
                .build(),
        )
    }
}
