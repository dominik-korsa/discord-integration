package com.dominikkorsa.discordintegration.listener

import com.dominikkorsa.discordintegration.DiscordIntegration
import com.dominikkorsa.discordintegration.config.EmbedConfig
import kotlinx.coroutines.delay
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerCountListener(private val plugin: DiscordIntegration) : Listener {
    private suspend fun sendStatus(
        content: String,
        player: Player,
        embedConfig: EmbedConfig
    ) {
        if (embedConfig.enabled) {
            val avatarUrl = plugin.avatarService.getAvatarUrl(player)
            plugin.client.sendWebhook {
                it.addEmbed { embed ->
                    embed.setTitle(content)
                    embed.setThumbnail(avatarUrl)
                    embed.setColor(embedConfig.color)
                }
            }
        } else plugin.client.sendWebhook(content)
        delay(500)
        plugin.client.updateActivity()
    }

    @EventHandler
    suspend fun onPlayerJoin(event: PlayerJoinEvent) {
        val content = plugin.discordFormatter.formatJoinInfo(event.player)
        sendStatus(content, event.player, plugin.configManager.joinEmbed)
    }

    @EventHandler
    suspend fun onPlayerQuit(event: PlayerQuitEvent) {
        val content = plugin.discordFormatter.formatQuitInfo(event.player)
        sendStatus(content, event.player, plugin.configManager.deathEmbed)
    }
}
