package com.dominikkorsa.discordintegration.plugin.service

import com.dominikkorsa.discordintegration.api.v1.DiscordIntegrationServiceV1
import com.dominikkorsa.discordintegration.api.v1.Linking
import com.dominikkorsa.discordintegration.plugin.DiscordIntegration
import discord4j.core.`object`.entity.Message
import org.bukkit.entity.Player

class ServiceV1(private val plugin: DiscordIntegration): DiscordIntegrationServiceV1 {
    override val minorAPIVersion = 0

    override val linking: Linking
        get() = plugin.linking

    override suspend fun reload() {
        plugin.reload()
    }

    override suspend fun sendChatToDiscord(player: Player, content: String) {
        plugin.webhooks.sendChatMessage(player, content)
    }

    override suspend fun sendJoinMessageToDiscord(player: Player) {
        plugin.webhooks.sendJoinMessage(player)
    }

    override suspend fun sendQuitMessageToDiscord(player: Player) {
        plugin.webhooks.sendQuitMessage(player)
    }

    override suspend fun sendDeathMessageToDiscord(player: Player, deathMessage: String?) {
        plugin.webhooks.sendDeathMessage(player, deathMessage)
    }

    override suspend fun sendDiscordMessageToChat(message: Message) {
        plugin.broadcastDiscordMessage(message)
    }
}
