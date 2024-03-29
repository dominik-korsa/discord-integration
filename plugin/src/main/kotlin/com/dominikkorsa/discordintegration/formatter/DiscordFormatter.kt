package com.dominikkorsa.discordintegration.formatter

import com.dominikkorsa.discordintegration.DiscordIntegration
import com.dominikkorsa.discordintegration.utils.floorBy
import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import java.text.DecimalFormat

class DiscordFormatter(val plugin: DiscordIntegration) {
    private val placeholderApiInstalled
        get() = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null

    private fun formatDeath(base: String, player: Player) = ChatColor.stripColor(base)
        .orEmpty()
        .replace("%player%", player.name)
        .replace("%pos-x%", player.location.blockX.toString())
        .replace("%pos-y%", player.location.blockY.toString())
        .replace("%pos-z%", player.location.blockZ.toString())

    fun formatDeathMessage(deathMessage: String?, player: Player): String {
        val baseMessage = deathMessage?.let {
            plugin.messages.discord.death
                .replace("%death-message%", it)
        } ?: plugin.messages.discord.deathFallback
        return formatDeath(baseMessage, player)
    }

    fun formatDeathEmbedTitle(player: Player) =
        formatDeath(plugin.messages.discord.deathEmbedTitle, player)

    fun formatActivity(
        players: Collection<Player>,
        maxPlayers: Int,
        time: Long
    ): String {
        val messageTemplate = when {
            players.isNotEmpty() -> plugin.messages.discordActivity
            else -> plugin.messages.discordActivityEmpty
        }

        val timeOfDay = ((time + 6000).mod((24000).toLong()) * 60 / 1000)
            .floorBy(plugin.configManager.activity.timeRound.toLong())
        val hour = timeOfDay / 60
        val minute = timeOfDay.mod(60)
        val pm = hour >= 12
        val minuteDisplay = DecimalFormat("00").format(minute)
        val timeDisplay =
            if (plugin.configManager.activity.time24h) "$hour:$minuteDisplay"
            else "${(hour - 1).mod(12) + 1}:$minuteDisplay ${if (pm) "PM" else "AM"}"

        var message = messageTemplate
            .replace("%online%", players.size.toString())
            .replace("%max%", maxPlayers.toString())
            .replace("%player-list%", players
                .map { player -> player.name }
                .sorted()
                .joinToString(", "))
            .replace("%time%", timeDisplay)
        if (placeholderApiInstalled) {
            message = PlaceholderAPI.setPlaceholders(null, message)
        }
        return message
    }

    fun formatJoinInfo(player: Player) = plugin.messages.discord.join
        .replace("%player%", player.name)

    fun formatQuitInfo(player: Player) = plugin.messages.discord.quit
        .replace("%player%", player.name)

    fun formatMessageContent(message: String): String = plugin.emojiFormatter
        .replaceEmojiNames(ChatColor.stripColor(message).orEmpty())
}
