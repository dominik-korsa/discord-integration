package com.dominikkorsa.discordintegration.listener

import com.dominikkorsa.discordintegration.DiscordIntegration
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent

class DeathListener(private val plugin: DiscordIntegration) : Listener {
    @EventHandler
    suspend fun onDeath(event: PlayerDeathEvent) {
        val content = plugin.discordFormatter.formatDeathMessage(event)
        if (plugin.configManager.deathEmbed.enabled) {
            val avatarUrl = plugin.avatarService.getAvatarUrl(event.entity)
            plugin.client.sendWebhook {
                it.addEmbed { embed ->
                    embed.setTitle(plugin.discordFormatter.formatDeathEmbedTitle(event))
                    embed.setDescription(content)
                    embed.setThumbnail(avatarUrl)
                    embed.setColor(plugin.configManager.deathEmbed.color)
                }
            }
        } else plugin.client.sendWebhook(content)
    }
}
