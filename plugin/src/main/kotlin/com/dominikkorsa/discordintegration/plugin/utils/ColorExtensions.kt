package com.dominikkorsa.discordintegration.plugin.utils

import discord4j.core.`object`.entity.PartialMember
import discord4j.core.`object`.entity.Role
import discord4j.core.util.OrderUtil
import discord4j.rest.util.Color
import kotlinx.coroutines.reactive.awaitFirstOrNull
import net.md_5.bungee.api.ChatColor
import reactor.math.MathFlux
import java.util.stream.Stream

fun Color.toHtml() = "#${rgb.toString(16).padStart(6, '0')}"

suspend fun PartialMember.getColorOrNull(): Color? =
    MathFlux.max(roles.filter { it.color != Role.DEFAULT_COLOR }, OrderUtil.ROLE_ORDER)
        .map { obj: Role -> obj.color }
        .awaitFirstOrNull()

fun extractColorCodes(input: String): Stream<String> =
    ChatColor.STRIP_COLOR_PATTERN.matcher(input).results().map { it.group() }
