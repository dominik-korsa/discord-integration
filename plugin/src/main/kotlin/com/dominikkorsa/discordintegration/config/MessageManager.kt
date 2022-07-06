package com.dominikkorsa.discordintegration.config

import com.dominikkorsa.discordintegration.DiscordIntegration
import dev.dejvokep.boostedyaml.block.implementation.Section
import dev.dejvokep.boostedyaml.route.Route

class MessageManager(plugin: DiscordIntegration): CustomConfig(plugin, "messages.yml") {

    class Minecraft(private val section: Section) {
        val message get() = section.getTrimmedString("message")
        val tooltip get() = section.getTrimmedString("tooltip")
        val defaultAuthorColor get() = section.getTrimmedString("default-author-color")
        val memberMentionContent get() = section.getTrimmedString("member-mention.content")
        val memberMentionTooltip get() = section.getTrimmedString("member-mention.tooltip")
        val memberMentionDefaultColor get() = section.getTrimmedString("member-mention.default-color")
        val roleMentionContent get() = section.getTrimmedString("role-mention.content")
        val roleMentionTooltip get() = section.getTrimmedString("role-mention.tooltip")
        val roleMentionDefaultColor get() = section.getTrimmedString("role-mention.default-color")
        val channelMentionContent get() = section.getTrimmedString("channel-mention.content")
        val channelMentionTooltip get() = section.getTrimmedString("channel-mention.tooltip")
        val kickMessage get() = section.getTrimmedString("linking.kick")
        val noCategory get() = section.getTrimmedString("no-category")
        val linkingClaimedByOther get() = section.getTrimmedString("linking.claimed-by-other-player")
        val linkingSuccess get() = section.getTrimmedString("linking.success")
        val updateMessage get() = section.getTrimmedString("update-notification.message")
        val updateLink get() = section.getTrimmedString("update-notification.link")
    }

    class Discord(private val section: Section) {
        val join get() = section.getTrimmedString("join")
        val quit get() = section.getTrimmedString("quit")
        val death get() = section.getTrimmedString("death")
        val deathFallback get() = section.getTrimmedString("death-fallback")
        val deathEmbedTitle get() = section.getTrimmedString("death-embed-title")
        val crashEmbedTitle get() = section.getTrimmedString("crash-embed.title")
        val crashEmbedContent get() = section.getTrimmedString("crash-embed.content")
        val crashEmbedLastOnline get() = section.getTrimmedString("crash-embed.last-online")
        val linkingSuccessTitle get() = section.getTrimmedString("linking.success.title")
        val linkingSuccessPlayerNameHeader get() = section.getTrimmedString("linking.success.player-name-header")
        val linkingUnknownCodeTitle get() = section.getTrimmedString("linking.unknown-code.title")
        val linkingUnknownCodeContent get() = section.getTrimmedString("linking.unknown-code.content")
        val profileInfoNotLinked get() = section.getTrimmedString("linking.profile-info.not-linked")
        val profileInfoError get() = section.getTrimmedString("linking.profile-info.error")
        val profileInfoTitle get() = section.getTrimmedString("linking.profile-info.title")
        val profileInfoPlayerNameHeader get() = section.getTrimmedString("linking.profile-info.player-name-header")
    }

    class Commands(private val section: Section) {
        val helpHeader get() = section.getTrimmedString("help.header")
        val helpCommand get() = section.getTrimmedString("help.command")
        val linkDisabled get() = section.getTrimmedString("link.disabled")
        val linkMessage get() = section.getTrimmedString("link.message")
        val linkCodeTooltip get() = section.getTrimmedString("link.code-tooltip")
        val unknown get() = section.getTrimmedString("unknown")
        val unlinkSuccess get() = section.getTrimmedString("unlink.success")
        val alreadyUnlinked get() = section.getTrimmedString("unlink.already-unlinked")
    }

    fun getCommandDescription(code: String) = config.getTrimmedString(Route.from("commands", "descriptions", code))

    val connected get() = config.getTrimmedString("connected")
    val connectionFailed get() = config.getTrimmedString("connection-failed")
    val discordActivity get() = config.getTrimmedString("discord-activity")
    val discordActivityEmpty get() = config.getTrimmedString("discord-activity-empty")
    val minecraft get() = Minecraft(config.getSection("minecraft"))
    val discord get() = Discord(config.getSection("discord"))
    val commands get() = Commands(config.getSection("commands"))
}
