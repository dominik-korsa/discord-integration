package com.dominikkorsa.discordintegration.plugin.listener

import com.dominikkorsa.discordintegration.plugin.DiscordIntegration
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent

class DeathListener(private val plugin: DiscordIntegration) : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    suspend fun onDeath(event: PlayerDeathEvent) {
        plugin.sendDeathMessageToDiscord(event)
    }
}
