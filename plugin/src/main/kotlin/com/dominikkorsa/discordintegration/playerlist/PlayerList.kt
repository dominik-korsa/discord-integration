package com.dominikkorsa.discordintegration.playerlist

import com.dominikkorsa.discordintegration.DiscordIntegration
import com.dominikkorsa.discordintegration.config.ConfigManager
import discord4j.core.spec.EmbedCreateSpec
import kotlinx.coroutines.delay
import org.bukkit.entity.Player

class PlayerList(private val plugin: DiscordIntegration) {
    private suspend fun sendStatus(
        content: String,
        player: Player,
        embedConfig: ConfigManager.Chat.EmbedOrMessage,
    ) {
        if (!embedConfig.enabled) return
        val webhookBuilder =
            if (embedConfig.playerAsAuthor) plugin.client.getPlayerWebhookBuilder(player)
            else plugin.client.getWebhookBuilder()
        if (embedConfig.asEmbed) {
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

    suspend fun onPlayerJoin(player: Player) {
        val content = plugin.discordFormatter.formatJoinInfo(player)
        sendStatus(content, player, plugin.configManager.chat.join)
    }

    suspend fun onPlayerLeave(player: Player) {
        val content = plugin.discordFormatter.formatQuitInfo(player)
        sendStatus(content, player, plugin.configManager.chat.quit)
    }
}
