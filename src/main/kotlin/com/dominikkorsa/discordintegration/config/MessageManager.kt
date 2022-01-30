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
    val minecraftTooltip get() = getString("minecraft.tooltip").trimEnd()
    val minecraftDefaultAuthorColor get() = getString("minecraft.default-author-color").trimEnd()
    val memberMentionContent get() = getString("minecraft.member-mention.content")
    val memberMentionTooltip get() = getString("minecraft.member-mention.tooltip").trimEnd()
    val memberMentionDefaultColor get() = getString("minecraft.member-mention.default-color").trimEnd()
    val roleMentionContent get() = getString("minecraft.role-mention.content")
    val roleMentionTooltip get() = getString("minecraft.role-mention.tooltip").trimEnd()
    val roleMentionDefaultColor get() = getString("minecraft.role-mention.default-color").trimEnd()
    val channelMentionContent get() = getString("minecraft.channel-mention.content")
    val channelMentionTooltip get() = getString("minecraft.channel-mention.tooltip").trimEnd()
    val noCategory get() = getString("minecraft.no-category")
    val discordJoin get() = getString("discord.join")
    val discordQuit get() = getString("discord.quit")
    val discordDeath get() = getString("discord.death")
    val discordDeathFallback get() = getString("discord.death-fallback")
    val discordDeathEmbedTitle get() = getString("discord.death-embed-title")
    val discordCrashEmbedTitle get() = getString("discord.crash-embed.title")
    val discordCrashEmbedContent get() = getString("discord.crash-embed.content")
    val discordCrashEmbedLastOnline get() = getString("discord.crash-embed.last-online")
    val commandsHelpHeader get() = getString("commands.help.header")
    val commandsHelpCommand get() = getString("commands.help.command")
    val commandsUnknown get() = getString("commands.unknown")
}
