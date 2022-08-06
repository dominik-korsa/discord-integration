package com.dominikkorsa.discordintegration.update

import com.dominikkorsa.discordintegration.DiscordIntegration
import com.dominikkorsa.discordintegration.api.Version
import com.dominikkorsa.discordintegration.response.GitHubRelease
import com.github.shynixn.mccoroutine.bukkit.launch
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.time.delay
import kotlinx.serialization.json.Json
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.time.Duration

class UpdateCheckerService(private val plugin: DiscordIntegration) {
    companion object {
        const val latestReleaseUrl = "https://api.github.com/repos/dominik-korsa/discord-integration/releases/latest"
        const val spigotResourceId = 91088
        val updateIdRegex = Regex("<!--.*spigot-update-id: *(\\d+).*-->")
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
        install(HttpCache)
    }
    private var job: Job? = null
    private var pendingUpdate: PendingUpdate? = null

    private fun hasPermission(player: Player) = player.hasPermission("discordintegration.notifyupdates")

    fun notify(player: Player) {
        pendingUpdate?.let { pendingUpdate ->
            if (!hasPermission(player)) return
            player.spigot().sendMessage(
                *plugin.minecraftFormatter.formatUpdateNotification(pendingUpdate).toTypedArray()
            )
        }
    }

    private fun notifyAll(pendingUpdate: PendingUpdate) {
        val message = plugin.minecraftFormatter.formatUpdateNotification(pendingUpdate)
        Bukkit.getOnlinePlayers().filter(::hasPermission).forEach { player ->
            player.spigot().sendMessage(*message.toTypedArray())
        }
        plugin.logger.info("§9New plugin version available!")
        plugin.logger.info("§9Current version: §7${pendingUpdate.currentVersion}")
        plugin.logger.info("§9Latest version:  §a${pendingUpdate.latestVersion}")
        plugin.logger.info("§9Download: §r${pendingUpdate.url}")
    }

    private suspend fun checkForUpdates() {
        try {
            val response: GitHubRelease = client.get(latestReleaseUrl).body()
            val githubVersion = Version.parse(response.tag_name)
            val currentVersion = Version.parse(plugin.description.version)

            if (!githubVersion.isNeverThan(currentVersion)) return
            var url = "https://www.spigotmc.org/resources/$spigotResourceId/"
            updateIdRegex.find(response.body)?.groups?.get(1)?.value?.let { updateId ->
                url += "download?version=$updateId"
            }
            val newPendingUpdate = PendingUpdate(
                plugin.description.version,
                response.tag_name.removePrefix("v"),
                url,
            )
            if (newPendingUpdate != pendingUpdate) notifyAll(newPendingUpdate)
            pendingUpdate = newPendingUpdate
        } catch (error: Exception) {
            plugin.logger.warning("Update check failed")
            plugin.logger.warning(error.message)
        }
    }

    fun start() {
        if (job !== null) return
        pendingUpdate = null
        job = plugin.launch {
            while (isActive) {
                checkForUpdates()
                delay(Duration.ofHours(4))
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
