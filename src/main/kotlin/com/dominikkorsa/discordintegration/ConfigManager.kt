package com.dominikkorsa.discordintegration

import com.dominikkorsa.discordintegration.exception.ConfigNotSetException

class ConfigManager(private val plugin: DiscordIntegration) {
    private val config get() = plugin.config

    init {
        plugin.saveDefaultConfig()
    }

    private fun getString(path: String): String {
        return config.getString(path) ?: throw ConfigNotSetException(path)
    }

    val discordToken get() = config.getString("discord-token") ?: throw ConfigNotSetException("discord-token")
    val chatChannels: List<String> get() = config.getStringList("chat.channels")
    val chatWebhooks: List<String> get() = config.getStringList("chat.webhooks")
    val avatarOfflineMode get() = config.getBoolean("chat.avatar.offline-mode")
    val avatarUrl get() = getString("chat.avatar.url")
}
