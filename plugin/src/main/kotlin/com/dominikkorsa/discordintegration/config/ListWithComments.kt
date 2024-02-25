package com.dominikkorsa.discordintegration.config

import com.dominikkorsa.discordintegration.DiscordIntegration
import java.io.File

class ListWithComments(
    private val plugin: DiscordIntegration,
    private val resourceFileName: String,
    fileName: String,
) {
    private val file = File(plugin.dataFolder, fileName)

    private fun initIfMissing() {
        if (file.exists()) return
        plugin.getResource(resourceFileName).copyTo(file.outputStream())
    }

    fun load(): List<String> {
        initIfMissing()
        return file.readLines()
            .map { it.trim() }
            .filterNot {
                it.isEmpty() || it.startsWith('#')
            }
    }

    fun add(values: List<String>) {
        val existingValues = load().toSet()
        val newValues = values - existingValues
        var text = file.readText()
        if (!text.endsWith("\n")) text += "\n"
        file.writeText(text + newValues.joinToString("\n") + "\n")
    }
}
