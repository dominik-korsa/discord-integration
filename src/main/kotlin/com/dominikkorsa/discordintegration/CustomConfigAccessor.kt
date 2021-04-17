package com.dominikkorsa.discordintegration

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.logging.Level

// https://gist.github.com/MrZoraman/a4dd438b75d48898f3b3
class CustomConfigAccessor(
    private val plugin: JavaPlugin,
    private val fileName: String
) {
    private val configFile = File(plugin.dataFolder, fileName)
    private var fileConfiguration: FileConfiguration? = null

    val config: FileConfiguration
        get() {
            if (fileConfiguration == null) reloadConfig()
            return fileConfiguration!!
        }

    fun reloadConfig() {
        fileConfiguration = YamlConfiguration.loadConfiguration(configFile)
        plugin.getResource(fileName)?.let { defConfigStream ->
            val defConfig = YamlConfiguration.loadConfiguration(InputStreamReader(defConfigStream))
            fileConfiguration?.setDefaults(defConfig)
        }
    }

    fun saveConfig() {
        try {
            config.save(configFile)
        } catch (ex: IOException) {
            plugin.logger.log(Level.SEVERE, "Could not save config to $configFile", ex)
        }
    }

    fun saveDefaultConfig() {
        if (!configFile.exists()) {
            plugin.saveResource(fileName, false)
        }
    }
}
