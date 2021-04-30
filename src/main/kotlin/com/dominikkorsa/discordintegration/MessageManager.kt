package com.dominikkorsa.discordintegration

import com.dominikkorsa.discordintegration.exception.MessageNotSetException

class MessageManager(plugin: DiscordIntegration) {
    private val configAccessor = CustomConfigAccessor(plugin, "messages.yml")
    private val config get() = configAccessor.config

    init {
        configAccessor.saveDefaultConfig()
    }

    private fun getString(path: String): String {
        return config.getString(path) ?: throw MessageNotSetException(path)
    }

    val connected get() = getString("connected")
    val discordActivity get() = getString("discord-activity")
    val discordActivityEmpty get() = getString("discord-activity-empty")
    val minecraftMessage get() = getString("minecraft.message")
    val minecraftTooltip get() = getString("minecraft.tooltip")
    val discordJoin get() = getString("discord.join")
    val discordQuit get() = getString("discord.quit")
    val discordDeath get() = getString("discord.death")
    val discordDeathFallback get() = getString("discord.death-fallback")
}
