package com.dominikkorsa.discordintegration.api.v1

@Suppress("unused")
interface UnlinkResult {
    /*
        Discord ID of previously linked user or null if no user was linked
     */
    val previouslyLinkedUserId: String?
}
