package com.dominikkorsa.discordintegration.plugin.listener

import com.dominikkorsa.discordintegration.plugin.DiscordIntegration
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerCountListener(private val plugin: DiscordIntegration) : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    suspend fun onPlayerJoin(event: PlayerJoinEvent) {
        plugin.playerList.onPlayerJoin(event.player)
        plugin.updateCheckerService.notify(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    suspend fun onPlayerQuit(event: PlayerQuitEvent) {
        plugin.playerList.onPlayerQuit(event.player)
    }
}
