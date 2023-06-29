package com.dominikkorsa.discordintegration.api.v1

import discord4j.core.`object`.entity.User
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.util.*

@Suppress("unused")
interface Linking {
    val isMandatory: Boolean

    fun generateLinkingCode(player: Player): LinkingCode

    suspend fun link(code: String, user: User): Player?
    suspend fun link(code: String, userId: String): Player?
    suspend fun link(offlinePlayer: OfflinePlayer, user: User)
    suspend fun link(offlinePlayer: OfflinePlayer, userId: String)

    /*
        Unlink player's Discord account
     */
    suspend fun unlink(player: OfflinePlayer): UnlinkResult

    fun getLinkedUserId(playerId: UUID): String?
    suspend fun getLinkedUser(playerId: UUID): User?
    fun getLinkedPlayer(discordId: String): OfflinePlayer?
}
