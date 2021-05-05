package com.dominikkorsa.discordintegration.listener

import com.dominikkorsa.discordintegration.DiscordIntegration
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent

class DeathListener(private val plugin: DiscordIntegration) : Listener {
    @EventHandler
    suspend fun onDeath(event: PlayerDeathEvent) {
        val content = plugin.discordFormatter.formatDeathMessage(event)
        plugin.client.sendBotMessage(content)
    }
}
