package com.dominikkorsa.discordintegration.config

import com.dominikkorsa.discordintegration.DiscordIntegration
import com.dominikkorsa.discordintegration.exception.ConfigNotSetException
import discord4j.rest.util.Color

class ConfigManager(private val plugin: DiscordIntegration) {
    private val config get() = plugin.config

    init {
        plugin.saveDefaultConfig()
    }

    fun reload() {
        plugin.reloadConfig()
    }

    private fun getString(path: String): String {
        return config.getString(path) ?: throw ConfigNotSetException(path)
    }

    private fun getInt(path: String): Int {
        return config.getInt(path)
    }

    private fun getEmbedConfig(path: String): EmbedConfig {
        return EmbedConfig(
            config.getBoolean("$path.enabled"),
            Color.of(config.getInt("$path.color"))
        )
    }

    val discordToken get() = getString("discord-token")
    val chatChannels: List<String> get() = config.getStringList("chat.channels")
    val chatWebhooks: List<String> get() = config.getStringList("chat.webhooks")
    val avatarOfflineMode get() = config.getBoolean("chat.avatar.offline-mode")
    val avatarUrl get() = getString("chat.avatar.url")
    val activityUpdateInterval get() = getInt("activity.update-interval")
    val activityTimeWorld get() = getString("activity.time.world")
    val activityTimeRound get() = getInt("activity.time.round")
    val activityTime24h get() = config.getBoolean("activity.time.24h")
    val joinEmbed get() = getEmbedConfig("chat.join-embed")
    val quitEmbed get() = getEmbedConfig("chat.quit-embed")
    val deathEmbed get() = getEmbedConfig("chat.death-embed")
}
