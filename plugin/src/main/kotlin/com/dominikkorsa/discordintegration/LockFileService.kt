package com.dominikkorsa.discordintegration

import com.github.shynixn.mccoroutine.bukkit.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.time.delay
import java.time.Duration
import java.util.*

class LockFileService(private val plugin: DiscordIntegration) {
    private var job: Job? = null
    private val file = plugin.dataFolder.resolve(".lock.txt")

    suspend fun start() {
        if (job !== null) return
        readFile()?.let { plugin.webhooks.notifyCrashed(it) }
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
}
