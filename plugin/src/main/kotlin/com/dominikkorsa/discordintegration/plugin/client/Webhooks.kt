package com.dominikkorsa.discordintegration.plugin.client

import com.dominikkorsa.discordintegration.plugin.DiscordIntegration
import com.dominikkorsa.discordintegration.plugin.config.ConfigManager
import com.dominikkorsa.discordintegration.plugin.utils.addEmbed
import discord4j.core.spec.WebhookExecuteSpec
import discord4j.rest.util.AllowedMentions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bukkit.entity.Player

class Webhooks(private val plugin: DiscordIntegration) {
    companion object {
        private val allowedMentionsNone = AllowedMentions.builder().build()
    }

    private suspend fun sendWebhook(
        player: Player?,
        build: WebhookExecuteSpec.Builder.() -> Unit,
    ) = coroutineScope {
        val spec = WebhookExecuteSpec.builder()
            .allowedMentions(allowedMentionsNone)
            .apply {
                if (player == null) return@apply
                username(player.name)
                avatarUrl(plugin.avatarService.getAvatarUrl(player))
            }
            .apply(build)
            .build()
        plugin.client.getWebhooks()?.map {
            async {
                it.execute(spec).awaitFirstOrNull()
            }
        }?.toList()?.awaitAll()
    }

    private suspend fun sendWebhook(build: WebhookExecuteSpec.Builder.() -> Unit) = sendWebhook(null, build)

    suspend fun notifyCrashed(timestamp: Long) {
        if (!plugin.configManager.chat.crashEmbed.enabled) return
        sendWebhook {
            addEmbed {
                title(plugin.messages.discord.crashEmbedTitle)
                description(plugin.messages.discord.crashEmbedContent)
                addField(
                    plugin.messages.discord.crashEmbedLastOnline,
                    "<t:${timestamp / 1000}>",
                    false
                )
                color(plugin.configManager.chat.crashEmbed.color)
            }
        }
    }

    suspend fun sendChatMessage(player: Player, content: String) {
        sendWebhook(player) {
            content(plugin.discordFormatter.formatMessageContent(content))
        }
    }

    suspend fun sendDynmapChatMessage(userName: String, content: String) {
        sendWebhook {
            username("[WEB] $userName")
            content(plugin.discordFormatter.formatMessageContent(content))
        }
    }

    suspend fun sendDeathMessage(player: Player, deathMessage: String?) {
        val embedConfig = plugin.configManager.chat.death
        if (!embedConfig.enabled) return

        val content = plugin.discordFormatter.formatDeathMessage(deathMessage, player)
        sendWebhook(if (embedConfig.playerAsAuthor) player else null) {
            if (embedConfig.asEmbed) addEmbed {
                title(plugin.discordFormatter.formatDeathEmbedTitle(player))
                description(content)
                color(embedConfig.color)
            }
            else content(content)
        }
    }

    private suspend fun sendStatus(
        content: String,
        player: Player,
        embedConfig: ConfigManager.Chat.EmbedOrMessage,
    ) {
        if (!embedConfig.enabled) return
        sendWebhook(if (embedConfig.playerAsAuthor) player else null) {
            if (embedConfig.asEmbed) addEmbed {
                title(content)
                color(embedConfig.color)
                build()
            }
            else content(content)
        }
    }

    suspend fun sendJoinMessage(player: Player) {
        val content = plugin.discordFormatter.formatJoinInfo(player)
        sendStatus(content, player, plugin.configManager.chat.join)
    }

    suspend fun sendQuitMessage(player: Player) {
        val content = plugin.discordFormatter.formatQuitInfo(player)
        sendStatus(content, player, plugin.configManager.chat.quit)
    }
}
