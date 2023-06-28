package com.dominikkorsa.discordintegration.plugin.listener

import com.dominikkorsa.discordintegration.plugin.DiscordIntegration
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.dynmap.DynmapWebChatEvent

class DynmapChatListener(private val plugin: DiscordIntegration) : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    suspend fun onDynmapChat(event: DynmapWebChatEvent) {
        plugin.webhooks.sendDynmapChatMessage(event.name, event.message)
    }
}
