package com.dominikkorsa.discordintegration.listener

import com.dominikkorsa.discordintegration.DiscordIntegration
import discord4j.core.spec.EmbedCreateSpec
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent

class DeathListener(private val plugin: DiscordIntegration) : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    suspend fun onDeath(event: PlayerDeathEvent) {
        val embedConfig = plugin.configManager.chat.death
        if (!embedConfig.enabled) return
        val content = plugin.discordFormatter.formatDeathMessage(event)
        val webhookBuilder =
            if (embedConfig.playerAsAuthor) plugin.client.getPlayerWebhookBuilder(event.entity)
            else plugin.client.getWebhookBuilder()
        if (embedConfig.asEmbed) {
            plugin.client.sendWebhook(
                webhookBuilder
                    .addEmbed(
                        EmbedCreateSpec.builder()
                            .title(plugin.discordFormatter.formatDeathEmbedTitle(event))
                            .description(content)
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
    }
}
