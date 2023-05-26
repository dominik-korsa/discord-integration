package com.dominikkorsa.discordintegration.imagemaps

import com.dominikkorsa.discordintegration.DiscordIntegration
import java.io.File
import java.nio.file.Path

/* This class copies over the temporary file to a permanent location using
* the filename provided by the user (modified to be lower and spaces repalced with _) */
class ImageMapsMigrator(private val plugin: DiscordIntegration) {

    fun migrateImage(file: Path, filename: String, basePath: String): Boolean {
        // will catch exceptions related to copying the file over (in case config is not setup correctly)
        try {
            // TODO: make basePath relative to server folder
            file.toFile().copyTo(File(basePath + filename))
        } catch (e: Exception) {
            plugin.logger.warning(e.toString())
            return false
        }
        return true
    }
}
