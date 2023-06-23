package com.dominikkorsa.discordintegration.utils

import discord4j.core.`object`.entity.channel.TopLevelGuildMessageChannel
import discord4j.core.util.PermissionUtil
import discord4j.rest.util.PermissionSet
import kotlinx.coroutines.reactive.awaitSingle

suspend fun TopLevelGuildMessageChannel.getEffectiveEveryonePermissions(): PermissionSet {
    val everyoneRole = guild.awaitSingle().everyoneRole.awaitSingle()
    return PermissionUtil.computePermissions(
        guild.awaitSingle().everyoneRole.awaitSingle().permissions,
        getOverwriteForRole(everyoneRole.id).orNull(),
        listOf(),
        null
    )
}
