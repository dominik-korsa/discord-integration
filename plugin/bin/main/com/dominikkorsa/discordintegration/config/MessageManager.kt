package com.dominikkorsa.discordintegration.config

import com.dominikkorsa.discordintegration.DiscordIntegration
import dev.dejvokep.boostedyaml.block.implementation.Section
import dev.dejvokep.boostedyaml.route.Route

class MessageManager(plugin: DiscordIntegration): CustomConfig(plugin, "messages.yml") {

    class Minecraft(private val section: Section) {
        val message get() = section.requireTrimmedString("message")
        val tooltip get() = section.requireTrimmedString("tooltip")
        val defaultAuthorColor get() = section.requireTrimmedString("default-author-color")
        val memberMentionContent get() = section.requireTrimmedString("member-mention.content")
        val memberMentionTooltip get() = section.requireTrimmedString("member-mention.tooltip")
        val memberMentionDefaultColor get() = section.requireTrimmedString("member-mention.default-color")
        val roleMentionContent get() = section.requireTrimmedString("role-mention.content")
        val roleMentionTooltip get() = section.requireTrimmedString("role-mention.tooltip")
        val roleMentionDefaultColor get() = section.requireTrimmedString("role-mention.default-color")
        val channelMentionContent get() = section.requireTrimmedString("channel-mention.content")
        val channelMentionTooltip get() = section.requireTrimmedString("channel-mention.tooltip")
        val kickMessage get() = section.requireTrimmedString("linking.kick")
        val noCategory get() = section.requireTrimmedString("no-category")
        val linkingClaimedByOther get() = section.requireTrimmedString("linking.claimed-by-other-player")
        val linkingSuccess get() = section.requireTrimmedString("linking.success")
        val updateMessage get() = section.requireTrimmedString("update-notification.message")
        val updateLink get() = section.requireTrimmedString("update-notification.link")
    }

    class Discord(private val section: Section) {
        val join get() = section.requireTrimmedString("join")
        val quit get() = section.requireTrimmedString("quit")
        val death get() = section.requireTrimmedString("death")
        val deathFallback get() = section.requireTrimmedString("death-fallback")
        val deathEmbedTitle get() = section.requireTrimmedString("death-embed-title")
        val crashEmbedTitle get() = section.requireTrimmedString("crash-embed.title")
        val crashEmbedContent get() = section.requireTrimmedString("crash-embed.content")
        val crashEmbedLastOnline get() = section.requireTrimmedString("crash-embed.last-online")
        val linkingSuccessTitle get() = section.requireTrimmedString("linking.success.title")
        val linkingSuccessPlayerNameHeader get() = section.requireTrimmedString("linking.success.player-name-header")
        val linkingUnknownCodeTitle get() = section.requireTrimmedString("linking.unknown-code.title")
        val linkingUnknownCodeContent get() = section.requireTrimmedString("linking.unknown-code.content")
        val profileInfoNotLinked get() = section.requireTrimmedString("linking.profile-info.not-linked")
        val profileInfoError get() = section.requireTrimmedString("linking.profile-info.error")
        val profileInfoTitle get() = section.requireTrimmedString("linking.profile-info.title")
        val profileInfoPlayerNameHeader get() = section.requireTrimmedString("linking.profile-info.player-name-header")
    }

    class Commands(private val section: Section) {
        val helpHeader get() = section.requireTrimmedString("help.header")
        val helpCommand get() = section.requireTrimmedString("help.command")
        val linkDisabled get() = section.requireTrimmedString("link.disabled")
        val linkMessage get() = section.requireTrimmedString("link.message")
        val linkCodeTooltip get() = section.requireTrimmedString("link.code-tooltip")
        val unknown get() = section.requireTrimmedString("unknown")
        val unlinkSuccess get() = section.requireTrimmedString("unlink.success")
        val alreadyUnlinked get() = section.requireTrimmedString("unlink.already-unlinked")
    }

    fun getCommandDescription(code: String) = config.requireTrimmedString(Route.from("commands", "descriptions", code))

    val connected get() = config.requireTrimmedString("connected")
    val connectionFailed get() = config.requireTrimmedString("connection-failed")
    val discordActivity get() = config.requireTrimmedString("discord-activity")
    val discordActivityEmpty get() = config.requireTrimmedString("discord-activity-empty")
    val minecraft get() = Minecraft(config.getSection("minecraft"))
    val discord get() = Discord(config.getSection("discord"))
    val commands get() = Commands(config.getSection("commands"))
}
