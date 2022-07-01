package com.dominikkorsa.discordintegration.linking

import com.dominikkorsa.discordintegration.DiscordIntegration
import com.github.shynixn.mccoroutine.launchAsync
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import org.bukkit.OfflinePlayer


class Linking(private val plugin: DiscordIntegration) {
    private val pendingConnections = HashMap<String, LinkingCode>()
    private val pendingConnectionQueue = Channel<LinkingCode>(8192)

    fun startJob() {
        plugin.launchAsync {
            pendingConnectionQueue.consumeEach {
                it.waitUntilInvalid()
                pendingConnections.remove(it.code)
            }
        }
    }

    suspend fun playerHasLinked(player: OfflinePlayer): Boolean {
        return plugin.db.getPlayer(player).discordId != null
    }

    fun generateLinkingCode(player: OfflinePlayer): LinkingCode {
        val allowedChars = ('a'..'z') + ('0'..'9')
        var code: String
        do {
            code = (1..6)
                .map { allowedChars.random() }
                .joinToString("")
        } while (pendingConnections.containsKey(code))
        val linkingCode = LinkingCode(code, player)
        pendingConnections[code] = linkingCode
        if (pendingConnectionQueue.trySend(linkingCode).isFailure) {
            pendingConnectionQueue.tryReceive().getOrNull()?.let {
                pendingConnections.remove(it.code)
            }
            pendingConnectionQueue.trySend(linkingCode).getOrThrow()
        }
        return linkingCode
    }
}
