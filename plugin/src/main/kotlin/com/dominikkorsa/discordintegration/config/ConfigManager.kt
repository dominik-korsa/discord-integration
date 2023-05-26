package com.dominikkorsa.discordintegration.config

import com.dominikkorsa.discordintegration.DiscordIntegration
import dev.dejvokep.boostedyaml.block.implementation.Section
import dev.dejvokep.boostedyaml.route.Route
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings
import java.util.regex.Pattern

class ConfigManager(plugin: DiscordIntegration) : CustomConfig(plugin, "config.yml") {
    override fun setUpdateSettings(builder: UpdaterSettings.Builder) {
        super.setUpdateSettings(builder)
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

    private fun fixStringList(route: Route) {
        if (config.isList(route)) return
        config.set(route, listOf(config.getString(route)))
    }

    private fun fixChannelListURL(route: Route) {
        config.set(route, config.getStringList(route).map {
            val matcher = channelUrlPattern.matcher(it)
            if (matcher.matches()) matcher.group(1) else it
        })
    }

    override fun applyFixes() {
        super.applyFixes()
        fixStringList(Route.from("chat", "channels"))
        fixStringList(Route.from("chat", "webhooks"))
        fixStringList(Route.from("chat", "console-channels"))
        fixChannelListURL(Route.from("chat", "channels"))
        fixChannelListURL(Route.from("chat", "console-channels"))
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
        val ignoreCancelledChatEvents get() = section.requireBoolean("ignore-cancelled-chat-events")
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
        enum class CancelledChatEventsMode {
            DISABLE,
            AUTO,
            ALL;

            companion object {
                fun parse(value: String): CancelledChatEventsMode? = when (value.lowercase()) {
                    "disable" -> DISABLE
                    "auto" -> AUTO
                    "all" -> ALL
                    else -> null
                }
            }
        }

        val logDiscordMessages get() = section.requireBoolean("log-discord-messages")
        val logCancelledChatEvents
            get() = CancelledChatEventsMode.parse(section.requireTrimmedString("log-cancelled-chat-events"))
                ?: throw Exception("debug.log-cancelled-chat-events config field only accepts values of: `disable`, `auto`, `all`")
        val imagemaps = section.requireBoolean("imagemaps")
    }

    val discordToken get() = config.getString("discord-token")?.trim()

    val chat get() = Chat(config.getSection("chat"))
    val activity get() = Activity(config.getSection("activity"))
    val linking get() = Linking(config.getSection("linking"))
    val debug get() = Debug(config.getSection("debug"))

    /* config for ImageMaps integration */
    class ImageMaps(private val section: Section) {
        val enabled get() = section.requireBoolean("enabled")
        val channels get() = section.requireStringList("channels")
        val path get() = section.requireTrimmedString("path")
    }
    val imagemaps get() = ImageMaps(config.getSection("imagemaps"))


    companion object {
        private val channelUrlPattern = Pattern.compile("""^https://(?:ptb\.)?discord\.com/channels/\d+/(\d+)$""")
    }
}
