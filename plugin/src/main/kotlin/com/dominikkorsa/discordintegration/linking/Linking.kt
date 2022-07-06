package com.dominikkorsa.discordintegration.linking

import com.dominikkorsa.discordintegration.DiscordIntegration
import com.github.shynixn.mccoroutine.bukkit.launch
import discord4j.core.`object`.entity.User
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player


class Linking(private val plugin: DiscordIntegration) {
    private val linkingCodes = HashMap<String, LinkingCode>()
    private val linkingCodeQueue = Channel<LinkingCode>(8192)

    val mandatory get() = plugin.configManager.linking.enabled && plugin.configManager.linking.mandatory

    fun startJob() {
        plugin.launch {
            linkingCodeQueue.consumeEach {
                it.waitUntilInvalid()
                linkingCodes.remove(it.code)
            }
        }
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
        plugin.db.setDiscordId(linkingCode.player.uniqueId, user.id)?.let { (previousPlayerId, previousDiscordId) ->
            previousPlayerId?.let(Bukkit::getPlayer)?.let {
                plugin.runTask {
                    it.kickPlayer(plugin.minecraftFormatter.formatClaimedByOtherMessage(linkingCode.player, user))
                }
            }
            previousDiscordId?.let { plugin.client.updateMember(it) }
            plugin.client.updateMember(user.id)
        }
        linkingCode.player.sendMessage(plugin.minecraftFormatter.formatLinkingSuccess(user))
        return linkingCode.player
    }

    suspend fun unlink(player: OfflinePlayer): Boolean {
        if (mandatory) player.player?.let(::kickPlayer)
        return plugin.db.resetDiscordId(player.uniqueId)
            ?.also { plugin.client.updateMember(it) } != null
    }

    private fun kickPlayer(player: Player) {
        val code = plugin.linking.generateLinkingCode(player)
        player.kickPlayer(plugin.messages.minecraft.kickMessage.replace("%code%", code.code))
    }

    suspend fun kickUnlinked() {
        if (!mandatory) return
        Bukkit.getOnlinePlayers()
            .asFlow()
            .filter { plugin.db.getDiscordId(it.uniqueId) == null }
            .collect(::kickPlayer)
    }
}
