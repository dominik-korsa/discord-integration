package com.dominikkorsa.discordintegration.api

import discord4j.core.`object`.entity.User
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.util.*

class LinkingAPIWrapper internal constructor(private val linking: LinkingAPI) : LinkingAPI {
    override val isMandatory get() = linking.isMandatory

    override fun generateLinkingCode(player: Player): String = linking.generateLinkingCode(player)

    override suspend fun link(code: String, user: User) = linking.link(code, user)
    override suspend fun link(code: String, userId: String) = linking.link(code, userId)
    override suspend fun link(offlinePlayer: OfflinePlayer, user: User) = linking.link(offlinePlayer, user)
    override suspend fun link(offlinePlayer: OfflinePlayer, userId: String) = linking.link(offlinePlayer, userId)

    override suspend fun unlink(player: OfflinePlayer) = linking.unlink(player)

    override fun getLinkedUserId(playerId: UUID) = linking.getLinkedUserId(playerId)
    override suspend fun getLinkedUser(playerId: UUID) = linking.getLinkedUser(playerId)
    override fun getLinkedPlayer(discordId: String) = linking.getLinkedPlayer(discordId)
}
