package com.dominikkorsa.discordintegration.listener

import com.dominikkorsa.discordintegration.DiscordIntegration
import org.bukkit.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent

class DeathListener(private val plugin: DiscordIntegration) : Listener {
    @EventHandler
    suspend fun onDeath(event: PlayerDeathEvent) {
        var deathMessage = event.deathMessage?.let {
            plugin.messageManager.discordDeath
                .replace("%death-message%", it)
        } ?: plugin.messageManager.discordDeathFallback
        deathMessage = ChatColor.stripColor(deathMessage) as String
        deathMessage = deathMessage
            .replace("%player%", event.entity.name)
            .replace("%pos-x%", event.entity.location.blockX.toString())
            .replace("%pos-y%", event.entity.location.blockY.toString())
            .replace("%pos-z%", event.entity.location.blockZ.toString())
        plugin.client.sendDeathInfo(deathMessage)
    }
}
