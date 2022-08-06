package com.dominikkorsa.discordintegration.plugin.replace

import java.util.regex.MatchResult
import java.util.regex.Pattern

data class Replacer<R>(
    val pattern: Pattern,
    val transform: (result: MatchResult) -> R,
) {
    companion object {
        fun <R> String.replaceTo(replacers: List<Replacer<R>>, remaining: (value: String) -> R): List<R> {
            val replacer = replacers.firstOrNull() ?: return listOf(remaining(this))
            val result = mutableListOf<R>()
            val matcher = replacer.pattern.matcher(this)
            var prevEnd = 0
            val nextReplacers = replacers.drop(1)
            while (matcher.find()) {
                result.addAll(substring(prevEnd, matcher.start()).replaceTo(nextReplacers, remaining))
                result.add(replacer.transform(matcher.toMatchResult()))
                prevEnd = matcher.end()
            }
            result.addAll(substring(prevEnd).replaceTo(nextReplacers, remaining))
            return result
        }
    }
}
