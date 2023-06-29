package com.dominikkorsa.discordintegration.plugin.listener

import com.dominikkorsa.discordintegration.api.v1.DiscordIntegrationMessageEvent
import com.dominikkorsa.discordintegration.plugin.DiscordIntegration
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class MessageEventListener(private val plugin: DiscordIntegration): Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    suspend fun onMessageEvent(event: DiscordIntegrationMessageEvent) {
        if (event.isCancelled) return
        plugin.broadcastDiscordMessage(event.message)
    }
}
