package com.dominikkorsa.discordintegration.utils

import discord4j.core.`object`.entity.channel.GuildMessageChannel
import discord4j.core.util.PermissionUtil
import discord4j.rest.util.PermissionSet
import kotlinx.coroutines.reactive.awaitSingle

suspend fun GuildMessageChannel.getEffectiveEveryonePermissions(): PermissionSet {
    val everyoneRole = guild.awaitSingle().everyoneRole.awaitSingle()
    return PermissionUtil.computePermissions(
        guild.awaitSingle().everyoneRole.awaitSingle().permissions,
        getOverwriteForRole(everyoneRole.id).orNull(),
        listOf(),
        null
    )
}
