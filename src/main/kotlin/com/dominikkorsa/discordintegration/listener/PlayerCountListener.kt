package com.dominikkorsa.discordintegration.listener

import com.dominikkorsa.discordintegration.DiscordIntegration
import com.dominikkorsa.discordintegration.config.EmbedConfig
import discord4j.core.spec.EmbedCreateSpec
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
            plugin.client.sendWebhook(
                plugin.client.getPlayerWebhookBuilder(player)
                    .addEmbed(EmbedCreateSpec.builder()
                        .title(content)
                        .color(embedConfig.color)
                        .build()
                    )
                    .build()
            )
        } else plugin.client.sendWebhook(
            plugin.client.getWebhookBuilder()
                .content(content)
                .build()
        )
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
        sendStatus(content, event.player, plugin.configManager.quitEmbed)
    }
}
