package com.dominikkorsa.discordintegration.formatter

import com.dominikkorsa.discordintegration.DiscordIntegration
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import java.util.regex.Pattern

class MinecraftFormatter(val plugin: DiscordIntegration) {
    private val unicodeToName = HashMap<String, List<String>>()
    private val unicodeEmojiPattern: Pattern
    private val serverEmojiPattern = Pattern.compile("<a?:(\\w+):\\d+>")

    init {
        val inputStream = plugin.getResource("emojis.txt") ?: throw Exception("Cannot load emojis.txt resource")
        inputStream.bufferedReader().forEachLine {
            if (it.isEmpty()) return@forEachLine
            val (key, names) = it.split(": ")
            unicodeToName[key] = names.split(", ")
        }
        unicodeEmojiPattern = Pattern.compile(unicodeToName.keys.joinToString("|") { Pattern.quote(it) })
    }

    suspend fun formatDiscordMessage(
        template: String,
        message: Message,
        channel: GuildMessageChannel
    ): String {
        val author = message.author.get()
        val nickname = message.authorAsMember.awaitFirstOrNull()?.displayName
            ?: author.username
        return template
            .replace("%username%", author.username)
            .replace("%user-tag%", author.tag)
            .replace("%nickname%", nickname)
            .replace("%channel-name%", channel.name)
            .replace("%channel-id%", channel.id.asString())
            .replace("%guild-name%", channel.guild.awaitFirst().name)
            .replace("%content%", replaceEmojis(message.content.trimEnd()))
    }

    fun formatHelpHeader(): String {
        return plugin.messageManager.commandsHelpHeader
            .replace("%plugin-version%", plugin.description.version)
    }

    fun formatHelpCommand(command: String, code: String): String {
        return plugin.messageManager.commandsHelpCommand
            .replace("%command%", command)
            .replace("%description%", plugin.messageManager.getCommandDescription(code))
    }

    private fun replaceUnicodeEmojis(input: String): String {
        return unicodeEmojiPattern.matcher(input).replaceAll { result ->
            unicodeToName[result.group()]?.let { ":${it.first()}:" } ?: result.group()
        }
    }

    private fun replaceGuildEmojis(input: String): String {
        return serverEmojiPattern.matcher(input).replaceAll { result -> ":${result.group(1)}:" }
    }

    private fun replaceEmojis(input: String): String {
        return replaceGuildEmojis(replaceUnicodeEmojis(input))
    }
}
