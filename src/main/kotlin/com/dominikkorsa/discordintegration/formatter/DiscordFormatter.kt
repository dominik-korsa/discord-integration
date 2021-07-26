package com.dominikkorsa.discordintegration.formatter

import com.dominikkorsa.discordintegration.DiscordIntegration
import com.dominikkorsa.discordintegration.tps.Tps
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.event.entity.PlayerDeathEvent
import java.text.DecimalFormat

class DiscordFormatter(val plugin: DiscordIntegration) {
    fun formatDeathMessage(event: PlayerDeathEvent): String {
        var deathMessage = event.deathMessage?.let {
            plugin.messageManager.discordDeath
                .replace("%death-message%", it)
        } ?: plugin.messageManager.discordDeathFallback
        deathMessage = ChatColor.stripColor(deathMessage) as String
        deathMessage = deathMessage
            .replace("%player%", event.entity.name)
            .replace("%pos-x%", event.entity.location.blockX.toString())
            .replace("%pos-y%", event.entity.location.blockY.toString())
            .replace("%pos-z%", event.entity.location.blockZ.toString())
        return deathMessage
    }

    fun formatActivity(
        players: Collection<Player>,
        maxPlayers: Int,
        tps: Tps
    ): String {
        val messageTemplate = when {
            players.isNotEmpty() -> plugin.messageManager.discordActivity
            else -> plugin.messageManager.discordActivityEmpty
        }

        val df = DecimalFormat("#0.00")
        return messageTemplate
            .replace("%online%", players.size.toString())
            .replace("%max%", maxPlayers.toString())
            .replace("%player-list%", players
                .map { player -> player.name }
                .sorted()
                .joinToString(", "))
            .replace("%tps-1m%", df.format(tps.of1min))
            .replace("%tps-5m%", df.format(tps.of5min))
            .replace("%tps-15m%", df.format(tps.of15min))
    }

    fun formatJoinInfo(player: Player): String {
        return plugin.messageManager.discordJoin
            .replace("%player%", player.name)
    }

    fun formatQuitInfo(player: Player): String {
        return plugin.messageManager.discordQuit
            .replace("%player%", player.name)
    }
}