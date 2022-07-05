package com.dominikkorsa.discordintegration.config

import com.dominikkorsa.discordintegration.DiscordIntegration
import dev.dejvokep.boostedyaml.block.implementation.Section
import dev.dejvokep.boostedyaml.route.Route

class MessageManager(plugin: DiscordIntegration): CustomConfig(plugin, "messages.yml") {

    class Minecraft(private val section: Section) {
        val message get() = section.getString("message")
        val tooltip get() = section.getString("tooltip").trimEnd()
        val defaultAuthorColor get() = section.getString("default-author-color").trimEnd()
        val memberMentionContent get() = section.getString("member-mention.content")
        val memberMentionTooltip get() = section.getString("member-mention.tooltip").trimEnd()
        val memberMentionDefaultColor get() = section.getString("member-mention.default-color").trimEnd()
        val roleMentionContent get() = section.getString("role-mention.content")
        val roleMentionTooltip get() = section.getString("role-mention.tooltip").trimEnd()
        val roleMentionDefaultColor get() = section.getString("role-mention.default-color").trimEnd()
        val channelMentionContent get() = section.getString("channel-mention.content")
        val channelMentionTooltip get() = section.getString("channel-mention.tooltip").trimEnd()
        val kickMessage get() = section.getString("linking.kick")
        val noCategory get() = section.getString("no-category")
        val linkingClaimedByOther get() = section.getString("linking.claimed-by-other-player")
        val linkingSuccess get() = section.getString("linking.success")
        val updateMessage get() = section.getString("update-notification.message")
        val updateLink get() = section.getString("update-notification.link")
    }

    class Discord(private val section: Section) {
        val join get() = section.getString("join")
        val quit get() = section.getString("quit")
        val death get() = section.getString("death")
        val deathFallback get() = section.getString("death-fallback")
        val deathEmbedTitle get() = section.getString("death-embed-title")
        val crashEmbedTitle get() = section.getString("crash-embed.title")
        val crashEmbedContent get() = section.getString("crash-embed.content")
        val crashEmbedLastOnline get() = section.getString("crash-embed.last-online")
        val linkingSuccessTitle get() = section.getString("linking.success.title")
        val linkingSuccessPlayerNameHeader get() = section.getString("linking.success.player-name-header")
        val linkingUnknownCodeTitle get() = section.getString("linking.unknown-code.title")
        val linkingUnknownCodeContent get() = section.getString("linking.unknown-code.content")
        val profileInfoNotLinked get() = section.getString("linking.profile-info.not-linked")
        val profileInfoError get() = section.getString("linking.profile-info.error")
        val profileInfoTitle get() = section.getString("linking.profile-info.title")
        val profileInfoPlayerNameHeader get() = section.getString("linking.profile-info.player-name-header")
    }

    class Commands(private val section: Section) {
        val helpHeader get() = section.getString("help.header")
        val helpCommand get() = section.getString("help.command")
        val linkDisabled get() = section.getString("link.disabled")
        val linkMessage get() = section.getString("link.message")
        val linkCodeTooltip get() = section.getString("link.code-tooltip")
        val unknown get() = section.getString("unknown")
        val unlinkSuccess get() = section.getString("unlink.success")
        val alreadyUnlinked get() = section.getString("unlink.already-unlinked")
    }

    fun getCommandDescription(code: String) = getString(Route.from("commands", "descriptions", code))

    val connected get() = getString("connected")
    val connectionFailed get() = getString("connection-failed")
    val discordActivity get() = getString("discord-activity")
    val discordActivityEmpty get() = getString("discord-activity-empty")
    val minecraft = Minecraft(config.getSection("minecraft"))
    val discord = Discord(config.getSection("discord"))
    val commands = Commands(config.getSection("commands"))
}
