package com.dominikkorsa.discordintegration.listener

import com.dominikkorsa.discordintegration.DiscordIntegration
import discord4j.core.spec.EmbedCreateSpec
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent

class DeathListener(private val plugin: DiscordIntegration) : Listener {
    @EventHandler
    suspend fun onDeath(event: PlayerDeathEvent) {
        val content = plugin.discordFormatter.formatDeathMessage(event)
        val webhookBuilder =
            if (plugin.configManager.chat.playerAsStatusAuthor) plugin.client.getPlayerWebhookBuilder(event.entity)
            else plugin.client.getWebhookBuilder()
        if (plugin.configManager.chat.deathEmbed.enabled) {
            plugin.client.sendWebhook(
                webhookBuilder
                    .addEmbed(
                        EmbedCreateSpec.builder()
                            .title(plugin.discordFormatter.formatDeathEmbedTitle(event))
                            .description(content)
                            .color(plugin.configManager.chat.deathEmbed.color)
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
