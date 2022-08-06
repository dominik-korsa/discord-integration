package com.dominikkorsa.discordintegration.listener

import com.dominikkorsa.discordintegration.DiscordIntegration
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerCountListener(private val plugin: DiscordIntegration) : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    suspend fun onPlayerJoin(event: PlayerJoinEvent) {
        plugin.sendJoinMessageToDiscord(event.player)
        plugin.updateCheckerService.notify(event.player)
        plugin.client.updateActivity()
    }

    @EventHandler(priority = EventPriority.MONITOR)
    suspend fun onPlayerQuit(event: PlayerQuitEvent) {
        plugin.sendQuitMessageToDiscord(event.player)
        plugin.client.updateActivity()
    }
}
