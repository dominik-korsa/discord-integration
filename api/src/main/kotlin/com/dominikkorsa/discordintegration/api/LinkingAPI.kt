package com.dominikkorsa.discordintegration.api

import discord4j.core.`object`.entity.User
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.util.*

interface LinkingAPI {
    val isMandatory: Boolean

    fun generateLinkingCode(player: Player): String

    suspend fun link(code: String, user: User): Player?
    suspend fun link(code: String, userId: String): Player?
    suspend fun link(offlinePlayer: OfflinePlayer, user: User)
    suspend fun link(offlinePlayer: OfflinePlayer, userId: String)

    /*
        Unlink player's Discord account
        Returns true if unlinking was successful and false if there was no account to unlink
     */
    suspend fun unlink(player: OfflinePlayer): Boolean

    fun getLinkedUserId(playerId: UUID): String?
    suspend fun getLinkedUser(playerId: UUID): User?
    fun getLinkedPlayer(discordId: String): OfflinePlayer?
}
