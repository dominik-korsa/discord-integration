package com.dominikkorsa.discordintegration.console

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
                event.level.isMoreSpecificThan(Level.ERROR) -> "‼️"
                event.level.isMoreSpecificThan(Level.WARN) -> "⚠️"
                event.level.isMoreSpecificThan(Level.INFO) -> "ℹ️"
                else -> "\uD83D\uDD27"
            }
            PatternLayout.createPatternParser(null).parse(buildString {
                append(emoji)
                append(" %p")
                if (event.loggerName.isNotEmpty()) append(" [%c]")
                append(""": ``%replace{%m}{`}{`​}``%n""")
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
