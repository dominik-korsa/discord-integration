package com.dominikkorsa.discordintegration.plugin.linking

import com.dominikkorsa.discordintegration.api.v1.Linking
import com.dominikkorsa.discordintegration.plugin.DiscordIntegration
import com.github.shynixn.mccoroutine.bukkit.launch
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.User
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.util.*


class Linking(private val plugin: DiscordIntegration) : Linking {
    private val linkingCodes = HashMap<String, LinkingCode>()
    private val linkingCodeQueue = Channel<LinkingCode>(8192)

    override val isMandatory get() = plugin.configManager.linking.enabled && plugin.configManager.linking.mandatory

    fun startJob() {
        plugin.launch {
            linkingCodeQueue.consumeEach {
                it.waitUntilInvalid()
                linkingCodes.remove(it.code)
            }
        }
    }

    override fun generateLinkingCode(player: Player): LinkingCode {
        val allowedChars = ('a'..'z') + ('0'..'9')
        var code: String
        do {
            code = (1..6).map { allowedChars.random() }.joinToString("")
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

    override suspend fun link(offlinePlayer: OfflinePlayer, user: User) {
        plugin.db.setDiscordId(offlinePlayer.uniqueId, user.id)?.let { (previousPlayerId, previousDiscordId) ->
            previousPlayerId?.let(Bukkit::getPlayer)?.let {
                plugin.runTask {
                    it.kickPlayer(plugin.minecraftFormatter.formatClaimedByOtherMessage(offlinePlayer, user))
                }
            }
            previousDiscordId?.let { plugin.client.updateMember(it) }
            plugin.client.updateMember(user.id)
        }
        offlinePlayer.player?.sendMessage(plugin.minecraftFormatter.formatLinkingSuccess(user))
    }

    override suspend fun link(code: String, user: User): Player? {
        val linkingCode = linkingCodes[code.lowercase()] ?: return null
        if (!linkingCode.isValid) return null
        linkingCode.use()
        link(linkingCode.player, user)
        return linkingCode.player
    }

    override suspend fun link(code: String, userId: String): Player? {
        val user = plugin.client.getUser(Snowflake.of(userId))
            ?: throw Error("User with id \"$userId\" not found")
        return link(code, user)
    }

    override suspend fun link(offlinePlayer: OfflinePlayer, userId: String) {
        val user = plugin.client.getUser(Snowflake.of(userId))
            ?: throw Error("User with id \"$userId\" not found")
        return link(offlinePlayer, user)
    }

    override suspend fun unlink(player: OfflinePlayer): UnlinkResult {
        if (isMandatory) player.player?.let(::kickPlayer)
        return UnlinkResult(
            plugin.db.resetDiscordId(player.uniqueId)?.also { plugin.client.updateMember(it) }
        )
    }

    override fun getLinkedUserId(playerId: UUID) =
        plugin.db.getDiscordId(playerId)?.asString()

    override suspend fun getLinkedUser(playerId: UUID) =
        plugin.db.getDiscordId(playerId)
            ?.let { plugin.client.getUser(it) }

    override fun getLinkedPlayer(discordId: String) =
        plugin.db.playerIdOfMember(Snowflake.of(discordId))
            ?.let(Bukkit::getOfflinePlayer)

    private fun kickPlayer(player: Player) {
        val code = plugin.linking.generateLinkingCode(player)
        player.kickPlayer(plugin.messages.minecraft.kickMessage.replace("%code%", code.code))
    }

    internal suspend fun kickUnlinked() {
        if (!isMandatory) return
        Bukkit.getOnlinePlayers().asFlow().filter(::shouldKick).collect(::kickPlayer)
    }

    internal fun shouldKick(player: Player) =
        isMandatory && !player.hasPermission("discordintegration.bypasslinking") && plugin.db.getDiscordId(player.uniqueId) == null
}
