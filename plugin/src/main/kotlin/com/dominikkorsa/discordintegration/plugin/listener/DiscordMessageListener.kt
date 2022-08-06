package com.dominikkorsa.discordintegration.plugin.listener

import com.dominikkorsa.discordintegration.api.DiscordIntegrationMessageEvent
import com.dominikkorsa.discordintegration.plugin.DiscordIntegration
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import java.time.Duration
import java.time.Instant.now

class DiscordMessageListener(private val plugin: DiscordIntegration) : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    suspend fun onDiscordMessage(event: DiscordIntegrationMessageEvent) {
        if (event.isCancelled) {
            if (plugin.configManager.debug.logDiscordMessages) plugin.logger.info(
                "Ignoring message, DiscordIntegrationMessageEvent has been cancelled by another plugin"
            )
            return
        }
        val timeStart = now()
        plugin.sendDiscordMessageToChat(event.message)
        if (plugin.configManager.debug.logDiscordMessages) plugin.logger.info(
            "Processing chat message took ${
                Duration.between(timeStart, now()).toMillis()
            } milliseconds"
        )
    }
}
