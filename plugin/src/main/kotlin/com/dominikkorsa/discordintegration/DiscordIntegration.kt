package com.dominikkorsa.discordintegration

import co.aikar.commands.PaperCommandManager
import com.dominikkorsa.discordintegration.command.DiscordIntegrationCommand
import com.dominikkorsa.discordintegration.compatibility.Compatibility
import com.dominikkorsa.discordintegration.config.ConfigManager
import com.dominikkorsa.discordintegration.config.MessageManager
import com.dominikkorsa.discordintegration.formatter.DiscordFormatter
import com.dominikkorsa.discordintegration.formatter.EmojiFormatter
import com.dominikkorsa.discordintegration.formatter.MinecraftFormatter
import com.dominikkorsa.discordintegration.linking.Linking
import com.dominikkorsa.discordintegration.listener.ChatListener
import com.dominikkorsa.discordintegration.listener.DeathListener
import com.dominikkorsa.discordintegration.listener.LoginListener
import com.dominikkorsa.discordintegration.listener.PlayerCountListener
import com.dominikkorsa.discordintegration.update.UpdateCheckerService
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.reactive.awaitFirstOrNull
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
    lateinit var messages: MessageManager
    private var activityJob: Job? = null
    private val connectionLock = Mutex()

    override fun onEnable() {
        super.onEnable()
        dataFolder.mkdirs()
        configManager = ConfigManager(this)
        messages = MessageManager(this)

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
        notifyInsecure()

        this.launch {
            db.init()
            linking.startJob()
            linking.kickUnlinked()
            lockFileService.start()
            connectionLock.withLock { connect() }
            updateCheckerService.start()
        }
    }

    override fun onDisable() {
        super.onDisable()
        lockFileService.stop()
        updateCheckerService.stop()
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
        notifyInsecure()
        updateCheckerService.stop()
        connectionLock.withLock {
            disconnect()
            connect()
        }
        updateCheckerService.start()
    }

    private fun initCommands() {
        val manager = PaperCommandManager(this)
        manager.registerCommand(DiscordIntegrationCommand(this))
    }

    private fun registerAllEvents() {
        registerSuspendingEvents(PlayerCountListener(this@DiscordIntegration))
        registerSuspendingEvents(ChatListener(this@DiscordIntegration))
        registerSuspendingEvents(DeathListener(this@DiscordIntegration))
        registerEvents(LoginListener(this@DiscordIntegration))
    }

    suspend fun broadcastDiscordMessage(message: Message) {
        val channel = message.channel.awaitFirstOrNull()
        if (channel == null || channel !is GuildMessageChannel) return
        val parts = minecraftFormatter.formatDiscordMessage(
            message,
            channel,
        ).toTypedArray()
        server.onlinePlayers.forEach {
            Compatibility.sendChatMessage(it, *parts)
        }
        Bukkit.getConsoleSender().sendMessage(TextComponent(*parts).toLegacyText())
    }

    private fun registerSuspendingEvents(listener: Listener) {
        server.pluginManager.registerSuspendingEvents(listener, this)
    }

    private fun registerEvents(listener: Listener) {
        server.pluginManager.registerEvents(listener, this)
    }

    private fun notifyInsecure() {
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
    }

    fun runTask(fn: () -> Unit) {
        Bukkit.getScheduler().runTask(this, Runnable(fn))
    }
}
