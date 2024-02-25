package com.dominikkorsa.discordintegration.config

import com.dominikkorsa.discordintegration.DiscordIntegration
import com.dominikkorsa.discordintegration.client.Webhooks
import com.dominikkorsa.discordintegration.utils.orNull
import com.dominikkorsa.discordintegration.utils.tryCast
import dev.dejvokep.boostedyaml.block.implementation.Section
import dev.dejvokep.boostedyaml.route.Route
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.*

class ConfigManager(plugin: DiscordIntegration) : CustomConfig(plugin, "config.yml") {
    override fun setUpdateSettings(builder: UpdaterSettings.Builder) {
        super.setUpdateSettings(builder)
        setUpdate5Settings(builder)
        setUpdate9Settings(builder)
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

    private fun setUpdate9Settings(builder: UpdaterSettings.Builder) {
        builder.apply {
            addRelocation("5", Route.from("chat", "crash-embed"), Route.from("chat", "crash"))
            listOf("join", "quit", "death", "crash").forEach { key ->
                addRelocation("5", Route.from("chat", key, "color"), Route.from("chat", key, "embed-color"))
            }

            addCustomLogic("9") {
                it.getTrimmedString("discord-token", "DISCORD_TOKEN_HERE")?.let { token ->
                    val file = File(plugin.dataFolder, "token.txt")
                    val newContents = plugin.getResource("token.txt")
                        .reader()
                        .readText()
                        .replace("DISCORD_BOT_TOKEN_HERE", token)
                    file.writeText(newContents)
                }
            }

            addCustomLogic("9") {
                val minecraftToDiscordIds =
                    it.requireStringList(Route.from("chat", "channels"), "CHANNEL_ID_HERE").toSet()

                val webhooksUrls = it.requireStringList(Route.from("chat", "webhooks"), "WEBHOOK_URL_HERE")
                ListWithComments(plugin, "webhooks.txt", "webhooks.txt").add(webhooksUrls)

                val discordToMinecraftIds = runBlocking {
                    webhooksUrls.map { webhookUrl ->
                        async {
                            val channelId: String?
                            try {
                                channelId = Webhooks.getWebhookChannelId(webhookUrl)
                            } catch (error: Exception) {
                                plugin.logger.warning("Failed to get webhook info of $webhookUrl")
                                plugin.logger.warning(error.message)
                                return@async null
                            }
                            if (channelId == null) {
                                plugin.logger.warning("Failed to get channel id from webhook $webhookUrl")
                                return@async null
                            }
                            channelId
                        }
                    }.awaitAll().filterNotNull()
                }.toHashSet()

                val newEntries = (minecraftToDiscordIds union discordToMinecraftIds).map { id ->
                    val discordToMinecraft = discordToMinecraftIds.contains(id)
                    val minecraftToDiscord = minecraftToDiscordIds.contains(id)
                    fun getEmbedValue(key: String) = when {
                        !minecraftToDiscord -> "disable"
                        !it.requireBoolean(Route.from("chat", key, "enabled")) -> "disable"
                        key == "crash-embed" -> "embed"
                        it.requireBoolean(Route.from("chat", key, "as-embed")) -> "embed"
                        else -> "message"
                    }
                    linkedMapOf(
                        "channel-id" to id,
                        "discord-to-minecraft" to if (discordToMinecraft) "enable" else "disable",
                        "minecraft-to-discord" to if (minecraftToDiscord) "enable" else "disable",
                        "join" to getEmbedValue("join"),
                        "quit" to getEmbedValue("quit"),
                        "death" to getEmbedValue("death"),
                        "crash" to getEmbedValue("crash-embed"),
                    )
                }

                if (newEntries.isEmpty()) return@addCustomLogic
                @Suppress("UNCHECKED_CAST")
                val channelList = it.getList(Route.from("chat", "message-channels")) as MutableList<Map<String, String>>
                channelList.clear()
                channelList.addAll(newEntries)
            }
        }
    }

    private fun fixStringList(route: Route) {
        if (config.isList(route)) return
        config.set(route, listOf(config.getString(route)))
    }

    private fun fixMessageChannels() {
        config.getList(Route.from("chat", "message-channels")).forEach {
            val map = it?.tryCast<MutableMap<String, Any>>() ?: return@forEach
            map["channel-id"]?.let { id ->
                if (id !is String) return@let
                channelUrlPattern.find(id)?.let { match ->
                    map["channel-id"] = match.groupValues[1]
                }
            }
            listOf("discord-to-minecraft", "minecraft-to-discord", "join", "quit", "death", "crash")
                .forEach { key -> map.putIfAbsent(key, "disable") }
        }
    }

    private fun fixConsoleChannelListURL() {
        val route = Route.from("chat", "console-channels")
        config.set(route, config.getStringList(route).map {
            channelUrlPattern.find(it)?.let { match ->
                match.groupValues[1]
            } ?: it
        })
    }

    override fun applyFixes() {
        super.applyFixes()
        fixStringList(Route.from("chat", "console-channels"))
        fixMessageChannels()
        fixConsoleChannelListURL()
    }

    override fun loadExtra() {
        super.loadExtra()
        val tokenFilename = config.requireTrimmedString(Route.from("files", "token"))
        discordToken = ListWithComments(plugin, "config.yml", tokenFilename, "DISCORD_BOT_TOKEN_HERE")
            .load()
            .singleOrNull()
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

        class ChatMessages(private val section: Section) {
            val enabled get() = section.requireBoolean("enabled")
        }

        val channels get() = section.requireStringList("channels", "CHANNEL_ID_HERE")
        val webhooks get() = section.requireStringList("webhooks", "WEBHOOK_ID_HERE")
        val consoleChannels get() = section.requireStringList("console-channels", "CHANNEL_ID_HERE")
        val avatarOfflineMode get() = section.requireBoolean("avatar.offline-mode")
        val avatarUrl get() = section.requireTrimmedString("avatar.url")
        val messages get() = ChatMessages(section.getSection("messages"))
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
        val linkedRoles get() = section.requireStringList("linked-roles", "ROLE_ID_HERE")
        val notLinkedRoles get() = section.requireStringList("not-linked-roles", "ROLE_ID_HERE")
        val syncNicknames get() = section.requireBoolean("sync-nicknames")
    }

    class DateTime(private val section: Section) {
        val timezone: TimeZone
            get() = section.getOptionalString("timezone").orNull()?.let {
                TimeZone.getTimeZone(it)
            } ?: TimeZone.getDefault()
        val is24h get() = section.requireBoolean("24h")
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
    }

    var discordToken: String? = null

    val chat get() = Chat(config.getSection("chat"))
    val activity get() = Activity(config.getSection("activity"))
    val linking get() = Linking(config.getSection("linking"))
    val dateTime get() = DateTime(config.getSection("date-time"))
    val debug get() = Debug(config.getSection("debug"))

    companion object {
        private val channelUrlPattern = Regex("""^https://(?:ptb\.)?discord\.com/channels/\d+/(\d+)$""")
    }
}
