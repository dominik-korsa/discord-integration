package com.dominikkorsa.discordintegration.config

import com.dominikkorsa.discordintegration.DiscordIntegration
import dev.dejvokep.boostedyaml.block.implementation.Section
import dev.dejvokep.boostedyaml.route.Route
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings

class ConfigManager(plugin: DiscordIntegration) : CustomConfig(plugin, "config.yml") {
    override fun setUpdateSettings(builder: UpdaterSettings.Builder) {
        setUpdate5Settings(builder)
    }

    private fun setUpdate5Settings(builder: UpdaterSettings.Builder) {
        builder.apply {
            fun addEmbedRelocation(key: String) {
                addRelocation("5", Route.from("chat", "${key}-embed"), Route.from("chat", key))
                addRelocation("5", Route.from("chat", key, "enabled"), Route.from("chat", key, "as-embed"))
                addCustomLogic("5") {
                    it.set(
                        Route.from("chat", key, "player-as-author"),
                        it.getBoolean(Route.from("chat", "player-as-status-author"))
                    )
                }
            }
            addEmbedRelocation("join")
            addEmbedRelocation("quit")
            addEmbedRelocation("death")
        }
    }

    class Chat(private val section: Section) {
        open class Embed(private val section: Section) {
            val enabled get() = section.requireBoolean("enabled")
            val color get() = section.getColor("color")
        }

        class EmbedOrMessage(private val section: Section) : Embed(section) {
            val asEmbed get() = section.requireBoolean("as-embed")
            val playerAsAuthor get() = section.requireBoolean("player-as-author")
        }

        val channels get() = section.requireStringList("channels")
        val webhooks get() = section.requireStringList("webhooks")
        val consoleChannels get() = section.requireStringList("console-channels")
        val avatarOfflineMode get() = section.requireBoolean("avatar.offline-mode")
        val avatarUrl get() = section.requireTrimmedString("avatar.url")
        val join get() = EmbedOrMessage(section.getSection("join"))
        val quit get() = EmbedOrMessage(section.getSection("quit"))
        val death get() = EmbedOrMessage(section.getSection("death"))
        val crashEmbed get() = Embed(section.getSection("crash-embed"))
    }

    class Activity(private val section: Section) {
        val updateInterval get() = section.requireInt("update-interval")
        val timeWorld get() = section.requireTrimmedString("time.world")
        val timeRound get() = section.requireInt("time.round")
        val time24h get() = section.requireBoolean("time.24h")
        val idle get() = section.requireBoolean("idle-when-no-players-online")
    }

    class Linking(private val section: Section) {
        val enabled get() = section.requireBoolean("enabled")
        val mandatory get() = section.requireBoolean("mandatory")
        val linkedRoles get() = section.requireStringList("linked-roles")
        val notLinkedRoles get() = section.requireStringList("not-linked-roles")
        val syncNicknames get() = section.requireBoolean("sync-nicknames")
    }

    class Debug(private val section: Section) {
        val logDiscordMessages get() = section.requireBoolean("log-discord-messages")
    }

    val discordToken get() = config.getString("discord-token")?.trim()

    val chat get() = Chat(config.getSection("chat"))
    val activity get() = Activity(config.getSection("activity"))
    val linking get() = Linking(config.getSection("linking"))
    val debug get() = Debug(config.getSection("debug"))
}
