package com.dominikkorsa.discordintegration.plugin.console

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.layout.PatternLayout
import org.apache.logging.log4j.core.pattern.RegexReplacement
import java.util.regex.Pattern

class ConsoleAppender(private val listener: (message: String) -> Unit) : AbstractAppender(
    name,
    null,
    PatternLayout.newBuilder()
        .withDisableAnsi(true)
        .withRegexReplacement(RegexReplacement.createRegexReplacement(pattern, ""))
        .withPatternSelector { event ->
            val emoji = when {
                event.level.isMoreSpecificThan(Level.ERROR) -> "\uD83D\uDFE5"
                event.level.isMoreSpecificThan(Level.WARN) -> "\uD83D\uDFE8"
                event.level.isMoreSpecificThan(Level.INFO) -> "\uD83D\uDFE6"
                else -> "⬜"
            }
            PatternLayout.createPatternParser(null).parse(buildString {
                append(emoji)
                append(" %p")
                if (event.loggerName.isNotBlank()) append(" [%c]")
                if (event.message.formattedMessage.isBlank()) append(":")
                else append(""": ``%replace{%m}{`}{`​}``""")
            }).toTypedArray()
        }
        .build(),
    false,
    emptyArray()
) {
    override fun getLayout() = super.getLayout() as PatternLayout

    companion object {
        const val name = "DiscordIntegrationConsoleAppender"
        private val pattern = Pattern.compile("""\x7F(?:#[0-f]{6}|\w)""")
    }

    override fun append(event: LogEvent) {
        listener(toSerializable(event) as String)
    }
}
