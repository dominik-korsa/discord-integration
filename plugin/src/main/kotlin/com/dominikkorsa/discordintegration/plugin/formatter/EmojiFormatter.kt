package com.dominikkorsa.discordintegration.plugin.formatter

import com.dominikkorsa.discordintegration.plugin.DiscordIntegration
import com.dominikkorsa.discordintegration.plugin.utils.replaceAll
import java.util.regex.Pattern

class EmojiFormatter(private val plugin: DiscordIntegration) {
    private val unicodeToName = HashMap<String, List<String>>()
    private val nameToMarkdown = HashMap<String, String>()
    private val unicodeEmojiPattern: Pattern
    private val serverEmojiPattern = Pattern.compile("<a?:(\\w+):\\d+>")
    private val emojiNamePattern = Pattern.compile(":(\\w+):")

    init {
        val inputStream = plugin.getResource("emojis.txt") ?: throw Exception("Cannot load emojis.txt resource")
        inputStream.bufferedReader().forEachLine { line ->
            if (line.isEmpty()) return@forEachLine
            val (key, nameString) = line.split(": ")
            val names = nameString.split(", ")
            unicodeToName[key] = names
            names.forEach { nameToMarkdown[it] = key }
        }
        unicodeEmojiPattern = Pattern.compile(unicodeToName.keys.joinToString("|") { Pattern.quote(it) })
    }

    private fun replaceUnicodeEmojis(input: String) = unicodeEmojiPattern.matcher(input)
        .replaceAll { result ->
            unicodeToName[result.group()]?.let { ":${it.first()}:" } ?: result.group()
        }

    private fun replaceGuildEmojis(input: String) = serverEmojiPattern.matcher(input)
        .replaceAll { result -> ":${result.group(1)}:" }

    fun replaceEmojis(input: String): String = replaceGuildEmojis(replaceUnicodeEmojis(input))

    fun replaceEmojiNames(input: String): String = emojiNamePattern.matcher(input).replaceAll {
        nameToMarkdown[it.group(1)]
            ?: plugin.client.getEmojiFormat(it.group(1))
            ?: it.group()
    }
}
