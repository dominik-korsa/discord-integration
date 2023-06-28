package com.dominikkorsa.discordintegration.plugin.formatter

import com.dominikkorsa.discordintegration.compatibility.Compatibility
import com.dominikkorsa.discordintegration.compatibility.Compatibility.setCopyToClipboard
import com.dominikkorsa.discordintegration.plugin.DiscordIntegration
import com.dominikkorsa.discordintegration.plugin.replace.Replacer
import com.dominikkorsa.discordintegration.plugin.replace.Replacer.Companion.replaceTo
import com.dominikkorsa.discordintegration.plugin.update.PendingUpdate
import com.dominikkorsa.discordintegration.plugin.utils.*
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.*
import discord4j.core.`object`.entity.channel.CategorizableChannel
import discord4j.core.`object`.entity.channel.Category
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import discord4j.rest.util.Color
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.entity.Player
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import kotlin.streams.toList


class MinecraftFormatter(val plugin: DiscordIntegration) {
    private fun formatRoleColor(role: Role) =
        role.color.takeUnless { it == Role.DEFAULT_COLOR }?.toHtml()?.let(Compatibility::hexChatColor)

    private fun String.formatUser(user: User, userColor: Color?, defaultColor: String) =
        plugin.emojiFormatter.replaceEmojis(
            replace("%username%", user.username)
                .replace("%user-tag%", user.tag)
                .replace("%user-id%", user.id.asString())
                .replace("%nickname%", if (user is Member) user.displayName else user.username)
                .replace("%user-color%", userColor?.toHtml()?.let(Compatibility::hexChatColor) ?: defaultColor)
        )

    private fun String.formatRole(role: Role) =
        plugin.emojiFormatter.replaceEmojis(
            replace("%role-name%", role.name)
                .replace("%role-id%", role.id.asString())
                .replace("%role-color%", formatRoleColor(role) ?: plugin.messages.minecraft.roleMentionDefaultColor)
        )

    private fun String.formatChannel(channel: GuildMessageChannel, category: Category?, guild: Guild) =
        plugin.emojiFormatter.replaceEmojis(
            replace("%channel-name%", channel.name)
                .replace("%channel-id%", channel.id.asString())
                .replace("%channel-category%", category?.name ?: plugin.messages.minecraft.noCategory)
                .replace("%guild-name%", guild.name)
        )

    private fun String.formatTime(time: Instant): String {
        val zonedTime = time.atZone(plugin.configManager.dateTime.timezone.toZoneId())
        val shortFormatter = DateTimeFormatter.ofPattern(when (plugin.configManager.dateTime.is24h) {
            true -> "HH:mm"
            false -> "hh:mm a"
        })
        val longFormatter = DateTimeFormatter.ofPattern(when (plugin.configManager.dateTime.is24h) {
            true -> "HH:mm:ss"
            false -> "hh:mm:ss a"
        })
        return replace("%time-short%", zonedTime.format(shortFormatter))
            .replace("%time-long%", zonedTime.format(longFormatter))
    }

    private suspend fun formatDiscordMessageContent(message: Message) = coroutineScope {
        plugin.emojiFormatter
            .replaceEmojis(message.content.trimEnd())
            .replaceTo(
                listOf(
                    Replacer(Pattern.compile("<@!?(\\d+)>")) {
                        async {
                            val guildMember = plugin.client.getMember(message.guildId.get(), Snowflake.of(it.group(1)))
                                ?: return@async listOf(TextComponent(it.group()))
                            val color = guildMember.getColorOrNull()
                            val component = TextComponent(
                                *TextComponent.fromLegacyText(
                                    plugin.messages.minecraft.memberMentionContent.formatUser(
                                        guildMember,
                                        color,
                                        plugin.messages.minecraft.memberMentionDefaultColor
                                    )
                                )
                            )
                            component.hoverEvent = HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                TextComponent.fromLegacyText(
                                    plugin.messages.minecraft.memberMentionTooltip.formatUser(
                                        guildMember,
                                        color,
                                        plugin.messages.minecraft.memberMentionDefaultColor
                                    )
                                )
                            )
                            listOf(component)
                        }
                    },
                    Replacer(Pattern.compile("<@&(\\d+)>")) {
                        async {
                            val role = plugin.client.getRole(message.guildId.get(), Snowflake.of(it.group(1)))
                                ?: return@async listOf(TextComponent(it.group()))
                            val component = TextComponent(
                                *TextComponent.fromLegacyText(
                                    plugin.messages.minecraft.roleMentionContent.formatRole(role)
                                )
                            )
                            component.hoverEvent = HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                TextComponent.fromLegacyText(
                                    plugin.messages.minecraft.roleMentionTooltip.formatRole(role)
                                )
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
                            val categoryDeferred = async {
                                channel.tryCast<CategorizableChannel>()?.category?.awaitFirstOrNull()
                            }
                            val guildDeferred = async { channel.guild.awaitSingle() }
                            val category = categoryDeferred.await()
                            val guild = guildDeferred.await()
                            val component = TextComponent(
                                *TextComponent.fromLegacyText(
                                    plugin.messages.minecraft.channelMentionContent.formatChannel(
                                        channel,
                                        category,
                                        guild,
                                    )
                                )
                            )
                            component.hoverEvent = HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                TextComponent.fromLegacyText(
                                    plugin.messages.minecraft.channelMentionTooltip.formatChannel(
                                        channel,
                                        category,
                                        guild,
                                    )
                                )
                            )
                            listOf(component)
                        }
                    },
                )
            ) {
                async {
                    if (it.isEmpty()) return@async listOf<TextComponent>()
                    else return@async TextComponent.fromLegacyText(it).toList()
                }
            }
            .awaitAll()
            .flatten()
    }

    private fun String.formatDiscordMessagePrefix(
        channel: GuildMessageChannel,
        channelCategory: Category?,
        guild: Guild,
        author: User,
        authorColor: Color?,
    ) = formatChannel(channel, channelCategory, guild)
        .formatUser(author, authorColor, plugin.messages.minecraft.defaultAuthorColor)

    suspend fun formatDiscordMessage(message: Message): List<BaseComponent> = coroutineScope {
        val channelDeferred = async {
            message.channel.awaitFirstOrNull()?.tryCast<GuildMessageChannel>()
                ?.let { it to it.tryCast<CategorizableChannel>()?.category?.awaitFirstOrNull() }
                ?: throw Exception("Cannot get message channel of message")
        }
        val authorDeferred = async {
            message.authorAsMember.awaitFirstOrNull()
                ?.let { it to it.getColorOrNull() }
                ?: (message.author.get() to null)
        }
        val guildDeferred = async { message.guild.awaitSingle() }
        val contentDeferred = async { formatDiscordMessageContent(message) }
        val (author, authorColor) = authorDeferred.await()
        val (channel, channelCategory) = channelDeferred.await()
        val guild = guildDeferred.await()
        val content = contentDeferred.await()
        val prefixHoverEvent = HoverEvent(
            HoverEvent.Action.SHOW_TEXT,
            TextComponent.fromLegacyText(
                plugin.messages.minecraft.tooltip
                    .formatDiscordMessagePrefix(channel, channelCategory, guild, author, authorColor)
            )
        )
        plugin.messages.minecraft.message
            .formatTime(message.timestamp)
            .split("%content%")
            .mapAndJoin({
                TextComponent(
                    *TextComponent.fromLegacyText(
                        it.formatDiscordMessagePrefix(channel, channelCategory, guild, author, authorColor)
                    )
                ).apply { hoverEvent = prefixHoverEvent }
            }, {
                TextComponent.fromLegacyText(extractColorCodes(it).toList().joinToString("")).last().apply {
                    content.forEach(::addExtra)
                }
            })
    }

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
