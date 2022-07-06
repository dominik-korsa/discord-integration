package com.dominikkorsa.discordintegration.listener

import com.dominikkorsa.discordintegration.DiscordIntegration
import com.dominikkorsa.discordintegration.config.ConfigManager
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
        embedConfig: ConfigManager.Chat.Embed,
    ) {
        val webhookBuilder =
            if (plugin.configManager.chat.playerAsStatusAuthor) plugin.client.getPlayerWebhookBuilder(player)
            else plugin.client.getWebhookBuilder()
        if (embedConfig.enabled) {
            plugin.client.sendWebhook(
                webhookBuilder
                    .addEmbed(
                        EmbedCreateSpec.builder()
                            .title(content)
                            .color(embedConfig.color)
                            .build()
                    )
                    .build()
            )
        } else plugin.client.sendWebhook(
            webhookBuilder
                .content(content)
                .build()
        )
        delay(500)
        plugin.client.updateActivity()
    }

    @EventHandler
    suspend fun onPlayerJoin(event: PlayerJoinEvent) {
        val content = plugin.discordFormatter.formatJoinInfo(event.player)
        sendStatus(content, event.player, plugin.configManager.chat.joinEmbed)
        plugin.updateCheckerService.notify(event.player)
    }

    @EventHandler
    suspend fun onPlayerQuit(event: PlayerQuitEvent) {
        val content = plugin.discordFormatter.formatQuitInfo(event.player)
        sendStatus(content, event.player, plugin.configManager.chat.quitEmbed)
    }
}
