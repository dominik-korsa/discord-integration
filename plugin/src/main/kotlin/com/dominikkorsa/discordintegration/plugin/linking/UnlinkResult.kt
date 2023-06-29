package com.dominikkorsa.discordintegration.plugin.linking

import com.dominikkorsa.discordintegration.api.v1.UnlinkResult
import discord4j.common.util.Snowflake

class UnlinkResult(previouslyLinkedUserId: Snowflake?) : UnlinkResult {
    override val previouslyLinkedUserId = previouslyLinkedUserId?.asString()

    internal val wasLinked get() = previouslyLinkedUserId !== null
}
