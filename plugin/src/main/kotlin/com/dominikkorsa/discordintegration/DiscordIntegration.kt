package com.dominikkorsa.discordintegration

import co.aikar.commands.PaperCommandManager
import com.dominikkorsa.discordintegration.command.DiscordIntegrationCommand
import com.dominikkorsa.discordintegration.compatibility.Compatibility
import com.dominikkorsa.discordintegration.config.ConfigManager
import com.dominikkorsa.discordintegration.config.MessageManager
import com.dominikkorsa.discordintegration.console.Console
import com.dominikkorsa.discordintegration.dynmap.DynmapIntegration
import com.dominikkorsa.discordintegration.exception.MissingIntentsException
import com.dominikkorsa.discordintegration.formatter.DiscordFormatter
import com.dominikkorsa.discordintegration.formatter.EmojiFormatter
import com.dominikkorsa.discordintegration.formatter.MinecraftFormatter
import com.dominikkorsa.discordintegration.linking.Linking
import com.dominikkorsa.discordintegration.listener.*
import com.dominikkorsa.discordintegration.luckperms.registerLuckPerms
import com.dominikkorsa.discordintegration.update.UpdateCheckerService
import com.dominikkorsa.discordintegration.utils.bunchLines
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import discord4j.core.`object`.entity.Message
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.time.delay
import kotlinx.coroutines.time.withTimeout
import net.md_5.bungee.api.chat.TextComponent
import org.bstats.bukkit.Metrics
import org.bstats.charts.SimplePie
import org.bukkit.Bukkit
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import java.time.Duration
import kotlin.time.toKotlinDuration

import com.dominikkorsa.discordintegration.imagemaps.FileScanner
import com.dominikkorsa.discordintegration.imagemaps.ImageMapsMigrator

class DiscordIntegration : JavaPlugin() {
    val client = Client(this)
    val discordFormatter = DiscordFormatter(this)
    val minecraftFormatter = MinecraftFormatter(this)
    val emojiFormatter = EmojiFormatter(this)
    val avatarService = AvatarService(this)
    val db = Db(this)
    val linking = Linking(this)
    private val lockFileService = LockFileService(this)
    val updateCheckerService = UpdateCheckerService(this)
    lateinit var configManager: ConfigManager
    /* two object classes needed for imagemaps */
    val imageMapMigrator = ImageMapsMigrator(this)
    val fileScanner = FileScanner(this)
    lateinit var messages: MessageManager
    private var dynmap: DynmapIntegration? = null
    private var activityJob: Job? = null
    private val connectionLock = Mutex()
    private val console = Console()

    override fun onEnable() {
        super.onEnable()
        dataFolder.mkdirs()
        configManager = ConfigManager(this)
        messages = MessageManager(this)
        dynmap = DynmapIntegration.create()

        val metrics = Metrics(this, 15660)
        metrics.addCustomChart(SimplePie("linking") {
            when {
                !configManager.linking.enabled -> "Disabled"
                configManager.linking.mandatory -> "Mandatory"
                else -> "Not mandatory"
            }
        })

        initCommands()
        registerAllEvents()
        startLogging()
        if (registerLuckPerms(this)) logger.info("Luck Perms integration enabled")
        else logger.info("Luck Perms not available")

        this.launch {
            db.init()
            linking.startJob()
            linking.kickUnlinked()
            lockFileService.start()
            connectionLock.withLock { connect() }
            showWarnings()
            updateCheckerService.start()
        }
    }

    override fun onDisable() {
        super.onDisable()
        lockFileService.stop()
        updateCheckerService.stop()
        console.stop()
        runBlocking {
            withTimeout(Duration.ofSeconds(5)) {
                connectionLock.withLock { disconnect() }
            }
        }
    }

    private suspend fun connect() {
        val token = configManager.discordToken
        if (token == null) {
            Bukkit.broadcastMessage(messages.connectionFailed)
            logger.severe("Field `discord-token` in config.yml has not been set")
            logger.severe(
                "Visit https://github.com/dominik-korsa/discord-integration/wiki/Configuring-a-Discord-bot for the configuration guide"
            )
            return
        }
        try {
            client.connect(token)
            this@DiscordIntegration.launch {
                client.initListeners()
            }
            activityJob = this@DiscordIntegration.launch {
                while (isActive) {
                    client.updateActivity()
                    delay(Duration.ofSeconds(configManager.activity.updateInterval.toLong()))
                }
            }
            Bukkit.broadcastMessage(messages.connected)
        } catch (error: MissingIntentsException) {
            Bukkit.broadcastMessage(messages.connectionFailed)
            """
                Missing intents!
                Please enabled the "Server members intent" and "Message content intent"
                in Discord bot settings:
                https://discord.com/developers/applications/${error.applicationId}/bot 
                
            """.trimIndent().lines().forEach(logger::severe)
        } catch (error: Exception) {
            Bukkit.broadcastMessage(messages.connectionFailed)
            error.printStackTrace()
        }
    }

    private suspend fun disconnect() {
        activityJob?.cancel()
        activityJob = null
        client.disconnect()
    }

    suspend fun reload() {
        configManager.reload()
        messages.reload()
        db.reload()
        linking.kickUnlinked()
        updateCheckerService.stop()
        connectionLock.withLock {
            disconnect()
            connect()
        }
        showWarnings()
        updateCheckerService.start()
    }

    private fun initCommands() {
        val manager = PaperCommandManager(this)
        manager.registerCommand(DiscordIntegrationCommand(this))
    }

    private fun registerAllEvents() {
        registerSuspendingEvents(PlayerCountListener(this))
        registerSuspendingEvents(ChatListener(this))
        registerSuspendingEvents(DeathListener(this))
        registerEvents(LoginListener(this))
        if (dynmap != null) registerSuspendingEvents(DynmapChatListener(this))
    }

    suspend fun broadcastDiscordMessage(message: Message) {
        val parts = minecraftFormatter.formatDiscordMessage(message).toTypedArray()
        server.onlinePlayers.forEach {
            Compatibility.sendChatMessage(it, *parts)
        }
        Bukkit.getConsoleSender().sendMessage(TextComponent(*parts).toLegacyText())
        dynmap?.sendMessage(TextComponent.toPlainText(*parts))
    }

    private fun registerSuspendingEvents(listener: Listener) {
        server.pluginManager.registerSuspendingEvents(listener, this)
    }

    private fun registerEvents(listener: Listener) {
        server.pluginManager.registerEvents(listener, this)
    }

    private fun showWarnings() {
        if (!Bukkit.getOnlineMode() && linking.mandatory) {
            """
                **** SERIOUS SECURITY ISSUE:
                Server is running in offline mode and mandatory linking is enabled
                Because the verification code is generated and displayed
                immediately after trying to join the server, this plugin
                lets players link before authenticating with a login plugin!
                This allows impersonators to link Minecraft profiles
                of other players to their Discord account.
            """.trimIndent().lineSequence().forEach(logger::severe)
        }
        if (configManager.chat.channels.isEmpty()) {
            """
                No Discord channels have been added in field `chat.channels`
                Put the IDs of the channels you want to be forwarded to Minecraft
                as entries in that list
                TIP: Enable `Advanced/Developer Mode` setting in Discord
                to get access to `Copy ID` feature in the right click menu
                
            """.trimIndent().lineSequence().forEach(logger::warning)
        }
        if (configManager.chat.webhooks.isEmpty()) {
            """
                No webhooks have been added in field `chat.webhooks`
                Visit https://github.com/dominik-korsa/discord-integration/wiki/Configuring-a-Discord-bot#configuring-webhooks
                for configuration instructions

            """.trimIndent().lineSequence().forEach(logger::warning)
        }
    }

    private fun startLogging() {
        launch {
            console.start()
                .bunchLines(Duration.ofMillis(200).toKotlinDuration(), 1900)
                .collect(::onLogMessage)
        }
    }

    private suspend fun onLogMessage(message: String) {
        client.sendConsoleMessage(message)
    }

    fun runConsoleCommand(command: String) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
    }

    fun runTask(fn: () -> Unit) {
        Bukkit.getScheduler().runTask(this, Runnable(fn))
    }
}
