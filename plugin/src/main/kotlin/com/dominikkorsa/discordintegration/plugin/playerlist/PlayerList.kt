package com.dominikkorsa.discordintegration.plugin.playerlist

import com.dominikkorsa.discordintegration.plugin.DiscordIntegration
import kotlinx.coroutines.delay
import org.bukkit.entity.Player

class PlayerList(private val plugin: DiscordIntegration) {
    private suspend fun updateActivity() {
        delay(500)
        plugin.client.updateActivity()
    }

    suspend fun onPlayerJoin(player: Player) {
        plugin.webhooks.sendJoinMessage(player)
        updateActivity()
    }

    suspend fun onPlayerQuit(player: Player) {
        plugin.webhooks.sendQuitMessage(player)
        updateActivity()
    }
}
