package com.dominikkorsa.discordintegration.plugin.utils

import java.util.regex.MatchResult
import java.util.regex.Matcher
import java.util.stream.Stream

fun Matcher.replaceAll(function: (result: MatchResult) -> String) = StringBuffer().also { result ->
    while (find()) appendReplacement(result, function(toMatchResult()))
    appendTail(result)
}.toString()

fun Matcher.results(): Stream<MatchResult> = Stream.builder<MatchResult>().also { builder ->
    while (find()) builder.add(toMatchResult())
}.build()
