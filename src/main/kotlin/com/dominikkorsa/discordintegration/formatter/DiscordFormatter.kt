package com.dominikkorsa.discordintegration.formatter

import com.dominikkorsa.discordintegration.DiscordIntegration
import com.dominikkorsa.discordintegration.tps.Tps
import com.dominikkorsa.discordintegration.utils.floorBy
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.event.entity.PlayerDeathEvent
import java.text.DecimalFormat

class DiscordFormatter(val plugin: DiscordIntegration) {
    private fun formatDeath(base: String, event: PlayerDeathEvent) = ChatColor.stripColor(base)
        .orEmpty()
        .replace("%player%", event.entity.name)
        .replace("%pos-x%", event.entity.location.blockX.toString())
        .replace("%pos-y%", event.entity.location.blockY.toString())
        .replace("%pos-z%", event.entity.location.blockZ.toString())

    fun formatDeathMessage(event: PlayerDeathEvent): String {
        val baseMessage = event.deathMessage?.let {
            plugin.messageManager.discordDeath
                .replace("%death-message%", it)
        } ?: plugin.messageManager.discordDeathFallback
        return formatDeath(baseMessage, event)
    }

    fun formatDeathEmbedTitle(event: PlayerDeathEvent) =
        formatDeath(plugin.messageManager.discordDeathEmbedTitle, event)

    fun formatActivity(
        players: Collection<Player>,
        maxPlayers: Int,
        tps: Tps,
        time: Long
    ): String {
        val messageTemplate = when {
            players.isNotEmpty() -> plugin.messageManager.discordActivity
            else -> plugin.messageManager.discordActivityEmpty
        }

        val timeOfDay = ((time + 6000).mod((24000).toLong()) * 60 / 1000)
            .floorBy(plugin.configManager.activityTimeRound.toLong())
        val hour = timeOfDay / 60
        val minute = timeOfDay.mod(60)
        val pm = hour >= 12
        val minuteDisplay = DecimalFormat("00").format(minute)
        val timeDisplay =
            if (plugin.configManager.activityTime24h) "$hour:$minuteDisplay"
            else "${(hour - 1).mod(12) + 1}:$minuteDisplay ${if (pm) "PM" else "AM"}"

        val tpsFormat = DecimalFormat("#0.00")
        return messageTemplate
            .replace("%online%", players.size.toString())
            .replace("%max%", maxPlayers.toString())
            .replace("%player-list%", players
                .map { player -> player.name }
                .sorted()
                .joinToString(", "))
            .replace("%tps-1m%", tpsFormat.format(tps.of1min))
            .replace("%tps-5m%", tpsFormat.format(tps.of5min))
            .replace("%tps-15m%", tpsFormat.format(tps.of15min))
            .replace("%time%", timeDisplay)
    }

    fun formatJoinInfo(player: Player) = plugin.messageManager.discordJoin
        .replace("%player%", player.name)

    fun formatQuitInfo(player: Player) = plugin.messageManager.discordQuit
        .replace("%player%", player.name)

    fun formatMessageContent(message: String): String = plugin.emojiFormatter
        .replaceEmojiNames(ChatColor.stripColor(message).orEmpty())
}
