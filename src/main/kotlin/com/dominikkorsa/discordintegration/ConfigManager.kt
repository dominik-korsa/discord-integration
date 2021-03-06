package com.dominikkorsa.discordintegration

import com.dominikkorsa.discordintegration.exception.ConfigNotSetException

class ConfigManager(private val plugin: DiscordIntegration) {
    private val config get() = plugin.config

    val discordToken get() = config.getString("discord-token") ?: throw ConfigNotSetException("discord-token")
    val activityMessage get() = config.getString("activity.message") ?: throw ConfigNotSetException("activity.message")
    val chatChannels: List<String> get() = config.getStringList("chat.channels")
    val chatWebhooks: List<String> get() = config.getStringList("chat.webhooks")
    val chatMinecraftMessage get() = config.getString("chat.minecraft-message") ?: throw ConfigNotSetException("chat.minecraft-message")
    val chatMinecraftTooltip get() = config.getString("chat.minecraft-tooltip") ?: throw ConfigNotSetException("chat.minecraft-tooltip")
    val chatDiscordJoin get() = config.getString("chat.discord-join") ?: throw ConfigNotSetException("chat.discord-join")
    val chatDiscordQuit get() = config.getString("chat.discord-quit") ?: throw ConfigNotSetException("chat.discord-quit")
    val charRenderHead get() = config.getBoolean("chat.render-head")
}
