package com.dominikkorsa.discordintegration.plugin

import com.github.shynixn.mccoroutine.bukkit.launch
import discord4j.core.spec.EmbedCreateSpec
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.time.delay
import java.time.Duration
import java.util.*

class LockFileService(private val plugin: DiscordIntegration) {
    private var job: Job? = null
    private val file = plugin.dataFolder.resolve("lock.txt")

    suspend fun start() {
        if (job !== null) return
        readFile()?.let { notifyCrashed(it) }
        job = plugin.launch {
            while (isActive) {
                updateFile()
                delay(Duration.ofSeconds(15))
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        file.delete()
    }

    private fun readFile(): Long? {
        if (!file.exists()) return null
        return file.readText().toLong()
    }

    private fun updateFile() {
        file.writeText(Date().time.toString())
    }

    private suspend fun notifyCrashed(timestamp: Long) {
        if (!plugin.configManager.chat.crashEmbed.enabled) return
        val webhookBuilder = plugin.client.getWebhookBuilder()
        plugin.client.sendWebhook(
            webhookBuilder
                .addEmbed(
                    EmbedCreateSpec.builder()
                        .title(plugin.messages.discord.crashEmbedTitle)
                        .description(plugin.messages.discord.crashEmbedContent)
                        .addField(
                            plugin.messages.discord.crashEmbedLastOnline,
                            "<t:${timestamp / 1000}>",
                            false
                        )
                        .color(plugin.configManager.chat.crashEmbed.color)
                        .build()
                )
                .build()
        )
    }
}
