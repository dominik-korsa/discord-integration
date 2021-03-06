package com.dominikkorsa.discordintegration

import kotlinx.coroutines.delay
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerCountListener(private val plugin: DiscordIntegration) : Listener {
    @EventHandler
    suspend fun onPlayerJoin(event: PlayerJoinEvent) {
        plugin.client.sendJoinInfo(event.player)
        delay(500)
        plugin.client.updatePlayerCount()
    }

    @EventHandler
    suspend fun onPlayerQuit(event: PlayerQuitEvent) {
        plugin.client.sendQuitInfo(event.player)
        delay(500)
        plugin.client.updatePlayerCount()
    }
}
