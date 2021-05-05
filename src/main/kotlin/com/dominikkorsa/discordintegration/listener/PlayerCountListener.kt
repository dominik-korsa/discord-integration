package com.dominikkorsa.discordintegration.listener

import com.dominikkorsa.discordintegration.DiscordIntegration
import kotlinx.coroutines.delay
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerCountListener(private val plugin: DiscordIntegration) : Listener {
    @EventHandler
    suspend fun onPlayerJoin(event: PlayerJoinEvent) {
        val content = plugin.discordFormatter.formatJoinInfo(event.player)
        plugin.client.sendBotMessage(content)
        delay(500)
        plugin.client.updateActivity()
    }

    @EventHandler
    suspend fun onPlayerQuit(event: PlayerQuitEvent) {
        val content = plugin.discordFormatter.formatQuitInfo(event.player)
        plugin.client.sendBotMessage(content)
        delay(500)
        plugin.client.updateActivity()
    }
}
