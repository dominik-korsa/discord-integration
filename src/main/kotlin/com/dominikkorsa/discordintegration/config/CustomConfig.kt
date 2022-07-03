package com.dominikkorsa.discordintegration.config

import com.dominikkorsa.discordintegration.exception.ConfigNotSetException
import dev.dejvokep.boostedyaml.YamlDocument
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning
import dev.dejvokep.boostedyaml.route.Route
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings
import discord4j.rest.util.Color
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

open class CustomConfig(
    plugin: JavaPlugin,
    private val fileName: String
) {
    protected val config: YamlDocument = YamlDocument.create(
        File(plugin.dataFolder, fileName),
        plugin.getResource(fileName) ?: throw Exception("Missing $fileName resource"),
        GeneralSettings.DEFAULT,
        LoaderSettings.DEFAULT,
        DumperSettings.DEFAULT,
        UpdaterSettings.builder().setVersioning(BasicVersioning("file-version")).build()
    )

    init { reload() }

    fun reload() {
        config.reload()
        config.update()
    }

    protected fun getString(route: String) = config.getString(route) ?: throw ConfigNotSetException(route, fileName)

    protected fun getString(route: Route) = config.getString(route) ?: throw ConfigNotSetException(route.toString(), fileName)

    protected fun getBoolean(route: String) = config.getBoolean(route) ?: throw ConfigNotSetException(route, fileName)

    protected fun getInt(route: String) = config.getInt(route) ?: throw ConfigNotSetException(route, fileName)

    protected fun getColor(router: String): Color = Color.of(Integer.decode(getString(router)))
}
