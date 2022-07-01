package com.dominikkorsa.discordintegration.linking

import com.dominikkorsa.discordintegration.DiscordIntegration
import com.dominikkorsa.discordintegration.entities.PlayerEntity
import com.dominikkorsa.discordintegration.entities.Players
import com.github.shynixn.mccoroutine.launchAsync
import discord4j.common.util.Snowflake
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction


class Linking(private val plugin: DiscordIntegration) {
    private val linkingCodes = HashMap<String, LinkingCode>()
    private val linkingCodeQueue = Channel<LinkingCode>(8192)

    fun startJob() {
        plugin.launchAsync {
            linkingCodeQueue.consumeEach {
                it.waitUntilInvalid()
                linkingCodes.remove(it.code)
            }
        }
    }

    suspend fun playerHasLinked(player: OfflinePlayer): Boolean {
        return plugin.db.getPlayer(player).discordId != null
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

    suspend fun link(code: String, discordId: Snowflake): Player? {
        val linkingCode = linkingCodes[code.lowercase()] ?: return null
        if (!linkingCode.isValid()) return null
        linkingCode.use()
        val dbPlayer = plugin.db.getPlayer(linkingCode.player)
        newSuspendedTransaction {
            // TODO: Remove role if discordId of previous player changed
            PlayerEntity.find { return@find Players.discordId eq discordId.asLong() }.singleOrNull()?.discordId = null
            dbPlayer.discordId = discordId.asLong()
        }
        return linkingCode.player
    }
}
