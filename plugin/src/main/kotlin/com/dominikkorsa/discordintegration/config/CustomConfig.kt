package com.dominikkorsa.discordintegration.config

import com.dominikkorsa.discordintegration.DiscordIntegration
import dev.dejvokep.boostedyaml.YamlDocument
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings
import java.io.File

open class CustomConfig(
    protected val plugin: DiscordIntegration,
    fileName: String,
) {
    protected val config: YamlDocument = YamlDocument.create(
        File(plugin.dataFolder, fileName),
        plugin.getResource(fileName) ?: throw Exception("Missing $fileName resource"),
        GeneralSettings.DEFAULT,
        LoaderSettings.DEFAULT,
        DumperSettings.DEFAULT,
        UpdaterSettings.builder().setVersioning(BasicVersioning("file-version")).also {
            this.setUpdateSettings(it)
        }.build()
    )

    init {
        reload()
    }

    protected open fun setUpdateSettings(builder: UpdaterSettings.Builder) {}

    protected open fun applyFixes() {}

    fun reload() {
        config.reload()
        applyFixes()
        config.update()
        config.save()
    }
}
