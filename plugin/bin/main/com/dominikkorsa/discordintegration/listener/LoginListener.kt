package com.dominikkorsa.discordintegration.listener

import com.dominikkorsa.discordintegration.DiscordIntegration
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLoginEvent

class LoginListener(private val plugin: DiscordIntegration): Listener {
    @EventHandler(priority = EventPriority.LOW)
    fun onLogin(event: PlayerLoginEvent) {
        if (!plugin.linking.shouldKick(event.player)) return
        val code = plugin.linking.generateLinkingCode(event.player)
        event.disallow(
            PlayerLoginEvent.Result.KICK_OTHER,
            plugin.messages.minecraft.kickMessage.replace("%code%", code.code)
        )
    }
}
