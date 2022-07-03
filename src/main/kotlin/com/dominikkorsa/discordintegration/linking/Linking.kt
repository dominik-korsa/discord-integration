package com.dominikkorsa.discordintegration.linking

import com.dominikkorsa.discordintegration.DiscordIntegration
import com.dominikkorsa.discordintegration.entities.PlayerEntity
import com.dominikkorsa.discordintegration.entities.Players
import com.github.shynixn.mccoroutine.bukkit.launch
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.User
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction


class Linking(private val plugin: DiscordIntegration) {
    private val linkingCodes = HashMap<String, LinkingCode>()
    private val linkingCodeQueue = Channel<LinkingCode>(8192)

    fun startJob() {
        plugin.launch {
            linkingCodeQueue.consumeEach {
                it.waitUntilInvalid()
                linkingCodes.remove(it.code)
            }
        }
    }

    suspend fun playerHasLinked(player: OfflinePlayer) = plugin.db.getPlayer(player).discordId != null

    suspend fun playerOfMember(discordId: Snowflake) = newSuspendedTransaction {
        PlayerEntity.find {
            Players.discordId eq discordId.asLong()
        }.firstOrNull()
    }

    fun generateLinkingCode(player: Player): LinkingCode {
        val allowedChars = ('a'..'z') + ('0'..'9')
        var code: String
        do {
            code = (1..6)
                .map { allowedChars.random() }
                .joinToString("")
        } while (linkingCodes.containsKey(code))
        val linkingCode = LinkingCode(code, player)
        linkingCodes[code] = linkingCode
        if (linkingCodeQueue.trySend(linkingCode).isFailure) {
            linkingCodeQueue.tryReceive().getOrNull()?.let {
                linkingCodes.remove(it.code)
            }
            linkingCodeQueue.trySend(linkingCode).getOrThrow()
        }
        return linkingCode
    }

    suspend fun link(code: String, user: User): Player? {
        val linkingCode = linkingCodes[code.lowercase()] ?: return null
        if (!linkingCode.isValid()) return null
        linkingCode.use()
        val dbPlayer = plugin.db.getPlayer(linkingCode.player)
        val previousId = newSuspendedTransaction {
            PlayerEntity
                .find { return@find (Players.discordId eq user.id.asLong()) and (Players.id neq dbPlayer.id) }
                .forEach {
                    plugin.runTask {
                        Bukkit.getPlayer(it.id.value)?.kickPlayer(
                            plugin.minecraftFormatter.formatClaimedByOtherMessage(linkingCode.player, user)
                        )
                    }
                    it.discordId = null
                }
            val previousId = dbPlayer.discordId
            dbPlayer.discordId = user.id.asLong()
            previousId
        }
        if (previousId != null) plugin.client.updateMember(Snowflake.of(previousId))
        plugin.client.updateMember(user.id)
        linkingCode.player.sendMessage(
            plugin.minecraftFormatter.formatLinkingSuccess(user)
        )
        return linkingCode.player
    }

    suspend fun unlink(player: OfflinePlayer): Boolean {
        val dbPlayer = plugin.db.getPlayer(player)
        val discordId = dbPlayer.discordId ?: return false
        newSuspendedTransaction {
            dbPlayer.discordId = null
        }
        plugin.client.updateMember(Snowflake.of(discordId))
        return true
    }
}
