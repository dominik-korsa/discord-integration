package com.dominikkorsa.discordintegration.api

import discord4j.core.`object`.entity.Message
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.entity.PlayerDeathEvent

@Suppress("unused")
class DiscordIntegrationAPIWrapper private constructor(
    private val plugin: DiscordIntegrationAPI,
    val version: Version,
) : DiscordIntegrationAPI {
    companion object {
        fun create(): DiscordIntegrationAPIWrapper? {
            val plugin = Bukkit.getPluginManager().getPlugin("DiscordIntegration") ?: return null
            val version = Version.parse(plugin.description.version)
            if (version.major < 4) throw UnsupportedVersionException(version)
            return DiscordIntegrationAPIWrapper(plugin as DiscordIntegrationAPI, version)
        }
    }

    override val linking = LinkingAPIWrapper(plugin.linking)

    override suspend fun reload() {
        plugin.reload()
    }

    override suspend fun sendChatToDiscord(player: Player, message: String) {
        plugin.sendChatToDiscord(player, message)
    }

    override suspend fun sendJoinMessageToDiscord(player: Player) {
        plugin.sendJoinMessageToDiscord(player)
    }

    override suspend fun sendQuitMessageToDiscord(player: Player) {
        plugin.sendQuitMessageToDiscord(player)
    }

    override suspend fun sendDeathMessageToDiscord(event: PlayerDeathEvent) {
        plugin.sendDeathMessageToDiscord(event)
    }

    override suspend fun sendDiscordMessageToChat(message: Message) {
        plugin.sendDiscordMessageToChat(message)
    }
}
