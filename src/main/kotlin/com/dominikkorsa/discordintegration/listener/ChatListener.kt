package com.dominikkorsa.discordintegration.listener

import com.dominikkorsa.discordintegration.AvatarService
import com.dominikkorsa.discordintegration.AvatarService.*
import com.dominikkorsa.discordintegration.DiscordIntegration
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent

class ChatListener(private val plugin: DiscordIntegration) : Listener {
    private val avatarService = AvatarService()

    @EventHandler
    suspend fun onPlayerChat(event: AsyncPlayerChatEvent) {
        val avatar = avatarService.getAvatarUrl(
            event.player,
            when (plugin.configManager.charRenderHead) {
                true -> AvatarType.Head
                false -> AvatarType.Face
            }
        )
        plugin.client.sendChatMessage(
            event.player.displayName,
            avatar,
            event.message,
        )
    }
}
