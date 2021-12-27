package com.dominikkorsa.discordintegration.config

import com.dominikkorsa.discordintegration.DiscordIntegration
import com.dominikkorsa.discordintegration.exception.MessageNotSetException

class MessageManager(plugin: DiscordIntegration) {
    private val configAccessor = CustomConfigAccessor(plugin, "messages.yml")
    private val config get() = configAccessor.config

    init {
        configAccessor.saveDefaultConfig()
    }

    fun reload() {
        configAccessor.reloadConfig()
    }

    private fun getString(path: String): String {
        return config.getString(path) ?: throw MessageNotSetException(path)
    }

    fun getCommandDescription(code: String): String {
        return getString("commands.descriptions.$code")
    }

    val connected get() = getString("connected")
    val connectionFailed get() = getString("connection-failed")
    val discordActivity get() = getString("discord-activity")
    val discordActivityEmpty get() = getString("discord-activity-empty")
    val minecraftMessage get() = getString("minecraft.message")
    val minecraftTooltip get() = getString("minecraft.tooltip")
    val discordJoin get() = getString("discord.join")
    val discordQuit get() = getString("discord.quit")
    val discordDeath get() = getString("discord.death")
    val discordDeathFallback get() = getString("discord.death-fallback")
    val discordDeathEmbedTitle get() = getString("discord.death-embed-title")
    val commandsHelpHeader get() = getString("commands.help.header")
    val commandsHelpCommand get() = getString("commands.help.command")
    val commandsUnknown get() = getString("commands.unknown")
}
