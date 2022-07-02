package com.dominikkorsa.discordintegration.config

import com.dominikkorsa.discordintegration.DiscordIntegration
import com.dominikkorsa.discordintegration.exception.MessageNotSetException

class MessageManager(plugin: DiscordIntegration) {
    private val configAccessor = CustomConfigAccessor(plugin, "messages.yml")
    private val config get() = configAccessor.config

    interface Minecraft {
        val message: String
        val tooltip: String
        val defaultAuthorColor: String
        val memberMentionContent: String
        val memberMentionTooltip: String
        val memberMentionDefaultColor: String
        val roleMentionContent: String
        val roleMentionTooltip: String
        val roleMentionDefaultColor: String
        val channelMentionContent: String
        val channelMentionTooltip: String
        val kickMessage: String
        val linkingClaimedByOther: String
        val noCategory: String
        val linkingSuccess: String
    }

    interface Discord {
        val join: String
        val quit: String
        val death: String
        val deathFallback: String
        val deathEmbedTitle: String
        val crashEmbedTitle: String
        val crashEmbedContent: String
        val crashEmbedLastOnline: String
    }

    interface Commands {
        val helpHeader: String
        val helpCommand: String
        val linkDisabled: String
        val linkMessage: String
        val linkCodeTooltip: String
        val unknown: String
    }

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
    val minecraft = object: Minecraft {
        override val message get() = getString("minecraft.message")
        override val tooltip get() = getString("minecraft.tooltip").trimEnd()
        override val defaultAuthorColor get() = getString("minecraft.default-author-color").trimEnd()
        override val memberMentionContent get() = getString("minecraft.member-mention.content")
        override val memberMentionTooltip get() = getString("minecraft.member-mention.tooltip").trimEnd()
        override val memberMentionDefaultColor get() = getString("minecraft.member-mention.default-color").trimEnd()
        override val roleMentionContent get() = getString("minecraft.role-mention.content")
        override val roleMentionTooltip get() = getString("minecraft.role-mention.tooltip").trimEnd()
        override val roleMentionDefaultColor get() = getString("minecraft.role-mention.default-color").trimEnd()
        override val channelMentionContent get() = getString("minecraft.channel-mention.content")
        override val channelMentionTooltip get() = getString("minecraft.channel-mention.tooltip").trimEnd()
        override val kickMessage get() = getString("minecraft.linking.kick")
        override val linkingClaimedByOther get() = getString("minecraft.linking.claimed-by-other-player")
        override val linkingSuccess get() = getString("minecraft.linking.success")
        override val noCategory get() = getString("minecraft.no-category")
    }
    val discord = object: Discord {
        override val join get() = getString("discord.join")
        override val quit get() = getString("discord.quit")
        override val death get() = getString("discord.death")
        override val deathFallback get() = getString("discord.death-fallback")
        override val deathEmbedTitle get() = getString("discord.death-embed-title")
        override val crashEmbedTitle get() = getString("discord.crash-embed.title")
        override val crashEmbedContent get() = getString("discord.crash-embed.content")
        override val crashEmbedLastOnline get() = getString("discord.crash-embed.last-online")
    }
    val commands = object: Commands {
        override val helpHeader get() = getString("commands.help.header")
        override val helpCommand get() = getString("commands.help.command")
        override val linkDisabled get() = getString("commands.link.disabled")
        override val linkMessage get() = getString("commands.link.message")
        override val linkCodeTooltip get() = getString("commands.link.code-tooltip")
        override val unknown get() = getString("commands.unknown")
    }
}
