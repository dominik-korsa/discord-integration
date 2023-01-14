package com.dominikkorsa.discordintegration.imagemaps

import com.dominikkorsa.discordintegration.DiscordIntegration
import discord4j.common.util.Snowflake
import java.io.File
import java.nio.file.Path

/* This class copies over the temporary file to a permanent location using
* the filename provided by the user (modified to be lower and spaces repalced with _) */
class ImageMapsMigrator(private val plugin: DiscordIntegration) {

    // config options
    var imenabled = plugin.configManager.imagemaps.imenabled
    var imchannels = plugin.configManager.imagemaps.imchannels.map(Snowflake::of)
    var imdebug = plugin.configManager.imagemaps.imdebug

    private var imPath = plugin.configManager.imagemaps.impath
    fun migrateImage(file: Path, filename: String) {
        // will catch exceptions related to copying the file over (in case config is not setup correctly)
        try {
            file.toFile().copyTo(File(imPath + filename))
        } catch (e: Exception) {
            plugin.logger.info(e.toString())
        }
    }
}