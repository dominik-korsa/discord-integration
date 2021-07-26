package com.dominikkorsa.discordintegration.listener

import com.dominikkorsa.discordintegration.AvatarService
import com.dominikkorsa.discordintegration.DiscordIntegration
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent

class ChatListener(private val plugin: DiscordIntegration) : Listener {
    private val avatarService = AvatarService(plugin)

    @EventHandler
    suspend fun onPlayerChat(event: AsyncPlayerChatEvent) {
        plugin.client.sendChatMessage(
            event.player.name,
            avatarService.getAvatarUrl(event.player),
            event.message,
        )
    }
}
