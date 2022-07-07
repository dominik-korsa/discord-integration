package com.dominikkorsa.discordintegration.formatter

import com.dominikkorsa.discordintegration.DiscordIntegration
import com.dominikkorsa.discordintegration.compatibility.Compatibility.setCopyToClipboard
import com.dominikkorsa.discordintegration.replace.Replacer
import com.dominikkorsa.discordintegration.replace.Replacer.Companion.replaceTo
import com.dominikkorsa.discordintegration.update.PendingUpdate
import com.dominikkorsa.discordintegration.utils.*
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.Role
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.CategorizableChannel
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.entity.Player
import java.util.regex.Pattern
import kotlin.streams.toList

class MinecraftFormatter(val plugin: DiscordIntegration) {
    private suspend fun formatUserOrMemberColor(user: User) = user
        .tryCast<Member>()?.getColorOrNull()?.toChatColor()

    private fun formatRoleColor(role: Role) =
        role.color.takeUnless { it == Role.DEFAULT_COLOR }?.toChatColor()

    private suspend fun formatUser(template: String, user: User, defaultColor: String) = template
        .replace("%username%", user.username)
        .replace("%user-tag%", user.tag)
        .replace("%user-id%", user.id.asString())
        .replace("%nickname%", if (user is Member) user.displayName else user.username)
        .replace("%user-color%", formatUserOrMemberColor(user) ?: defaultColor)

    private fun formatRole(template: String, role: Role) = template
        .replace("%role-name%", role.name)
        .replace("%role-id%", role.id.asString())
        .replace("%role-color%", formatRoleColor(role) ?: plugin.messages.minecraft.roleMentionDefaultColor)

    private suspend fun formatChannel(template: String, channel: GuildMessageChannel) = template
        .replace("%channel-name%", channel.name)
        .replace("%channel-id%", channel.id.asString())
        .replace(
            "%channel-category%",
            channel.tryCast<CategorizableChannel>()?.category?.awaitFirstOrNull()?.name
                ?: plugin.messages.minecraft.noCategory
        )
        .replace("%guild-name%", channel.guild.awaitFirst().name)

    private suspend fun formatDiscordMessageContent(
        message: Message,
        messageChannel: GuildMessageChannel,
        hover: Boolean,
    ) = coroutineScope {
        plugin.emojiFormatter
            .replaceEmojis(message.content.trimEnd())
            .replaceTo(listOf(
                Replacer(Pattern.compile("<@!?(\\d+)>")) {
                    async {
                        val guildMember = plugin.client.getMember(messageChannel.guildId, Snowflake.of(it.group(1)))
                            ?: return@async listOf(TextComponent(it.group()))
                        val component = TextComponent(*TextComponent.fromLegacyText(formatUser(
                            plugin.messages.minecraft.memberMentionContent,
                            guildMember,
                            plugin.messages.minecraft.memberMentionDefaultColor
                        )))
                        if (hover) component.hoverEvent = HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            TextComponent.fromLegacyText(
                                formatUser(
                                    plugin.messages.minecraft.memberMentionTooltip,
                                    guildMember,
                                    plugin.messages.minecraft.memberMentionDefaultColor
                                )
                            )
                        )
                        listOf(component)
                    }
                },
                Replacer(Pattern.compile("<@&(\\d+)>")) {
                    async {
                        val role = plugin.client.getRole(messageChannel.guildId, Snowflake.of(it.group(1)))
                            ?: return@async listOf(TextComponent(it.group()))
                        val component = TextComponent(*TextComponent.fromLegacyText(
                            formatRole(plugin.messages.minecraft.roleMentionContent, role)
                        ))
                        if (hover) component.hoverEvent = HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            TextComponent.fromLegacyText(formatRole(plugin.messages.minecraft.roleMentionTooltip, role))
                        )
                        listOf(component)
                    }
                },
                Replacer(Pattern.compile("<#(\\d+)>")) {
                    async {
                        // TODO: Add support for threads
                        // See: https://github.com/Discord4J/Discord4J/issues/958
                        val channel = plugin.client.getChannel(Snowflake.of(it.group(1)))
                            ?.tryCast<GuildMessageChannel>()
                            ?: return@async listOf(TextComponent(it.group()))
                        val component = TextComponent(*TextComponent.fromLegacyText(
                            formatChannel(plugin.messages.minecraft.channelMentionContent, channel)
                        ))
                        if (hover) component.hoverEvent = HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            TextComponent.fromLegacyText(
                                formatChannel(
                                    plugin.messages.minecraft.channelMentionTooltip,
                                    channel
                                )
                            )
                        )
                        listOf(component)
                    }
                },
            )) { async {
                if (it.isEmpty()) return@async listOf<TextComponent>() else return@async TextComponent.fromLegacyText(it).toList()
            }}
            .awaitAll()
            .flatten()
    }

    private suspend fun formatDiscordMessage(
        template: String,
        message: Message,
        channel: GuildMessageChannel,
        prefixHoverEvent: HoverEvent?,
        contentHover: Boolean,
    ): List<BaseComponent> {
        val author = message.author.get()
        val content = formatDiscordMessageContent(message, channel, contentHover)
        var result = formatChannel(template, channel)
        result = formatUser(
            result,
            message.authorAsMember.awaitFirstOrNull() ?: author,
            plugin.messages.minecraft.defaultAuthorColor
        )
        return result
            .split("%content%")
            .mapAndJoin({
                TextComponent(*TextComponent.fromLegacyText(it)).apply {
                    hoverEvent = prefixHoverEvent
                }
            }, {
                TextComponent.fromLegacyText(extractColorCodes(it).toList().joinToString("")).last().apply {
                    content.forEach(::addExtra)
                }
            })
    }

    suspend fun formatDiscordMessage(
        message: Message,
        channel: GuildMessageChannel,
    ) = formatDiscordMessage(
        plugin.messages.minecraft.message,
        message,
        channel,
        HoverEvent(
            HoverEvent.Action.SHOW_TEXT,
            plugin.minecraftFormatter.formatDiscordMessage(
                plugin.messages.minecraft.tooltip,
                message,
                channel,
                null,
                false
            ).toTypedArray()
        ),
        true,
    )

    fun formatHelpHeader() = plugin.messages.commands.helpHeader
        .replace("%plugin-version%", plugin.description.version)

    fun formatHelpCommand(command: String, code: String) = plugin.messages.commands.helpCommand
        .replace("%command%", command)
        .replace("%description%", plugin.messages.getCommandDescription(code))

    fun formatLinkCommandMessage(code: String) = plugin.messages.commands.linkMessage
        .split("%code%")
        .mapAndJoin({ TextComponent(*TextComponent.fromLegacyText(it)) }, {
            TextComponent.fromLegacyText(extractColorCodes(it).toList().joinToString("")).last().apply {
                addExtra(code)
                setCopyToClipboard(code, TextComponent.fromLegacyText(plugin.messages.commands.linkCodeTooltip))
            }
        })

    fun formatClaimedByOtherMessage(player: Player, user: User) = plugin.messages.minecraft.linkingClaimedByOther
        .replace("%player-name%", player.name)
        .replace("%user-tag%", user.tag)

    fun formatLinkingSuccess(user: User) = plugin.messages.minecraft.linkingSuccess
        .replace("%user-tag%", user.tag)

    fun formatUpdateNotification(pendingUpdate: PendingUpdate) = plugin.messages.minecraft.updateMessage
        .replace("%current-version%", pendingUpdate.currentVersion)
        .replace("%latest-version%", pendingUpdate.latestVersion)
        .replace("%url%", pendingUpdate.url)
        .split("%link%")
        .mapAndJoin({ TextComponent(*TextComponent.fromLegacyText(it)) }, {
            TextComponent(*TextComponent.fromLegacyText(plugin.messages.minecraft.updateLink)).apply {
                clickEvent = ClickEvent(ClickEvent.Action.OPEN_URL, pendingUpdate.url)
                hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, arrayOf(TextComponent(pendingUpdate.url)))
            }
        })
}
