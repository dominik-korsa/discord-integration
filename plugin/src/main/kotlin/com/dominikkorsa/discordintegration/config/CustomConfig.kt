package com.dominikkorsa.discordintegration.config

import dev.dejvokep.boostedyaml.YamlDocument
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning
import dev.dejvokep.boostedyaml.libs.org.snakeyaml.engine.v2.common.ScalarStyle
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

open class CustomConfig(
    plugin: JavaPlugin,
    fileName: String,
) {
    protected val config: YamlDocument = YamlDocument.create(
        File(plugin.dataFolder, fileName),
        plugin.getResource(fileName) ?: throw Exception("Missing $fileName resource"),
        GeneralSettings.DEFAULT,
        LoaderSettings.DEFAULT,
        DumperSettings.builder().setScalarStyle(ScalarStyle.LITERAL).build(),
        UpdaterSettings.builder().setVersioning(BasicVersioning("file-version")).build()
    )

    init { reload() }

    fun reload() {
        config.reload()
        config.update()
    }
}
