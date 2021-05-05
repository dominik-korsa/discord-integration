package com.dominikkorsa.discordintegration

import co.aikar.commands.PaperCommandManager
import com.dominikkorsa.discordintegration.commands.DiscordIntegrationCommand
import com.dominikkorsa.discordintegration.config.ConfigManager
import com.dominikkorsa.discordintegration.config.MessageManager
import com.dominikkorsa.discordintegration.listener.ChatListener
import com.dominikkorsa.discordintegration.listener.DeathListener
import com.dominikkorsa.discordintegration.listener.PlayerCountListener
import com.github.shynixn.mccoroutine.launchAsync
import com.github.shynixn.mccoroutine.registerSuspendingEvents
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

class DiscordIntegration: JavaPlugin() {
    val client = Client(this)
    lateinit var configManager: ConfigManager
    lateinit var messageManager: MessageManager
    var activityJob: Job? = null

    override fun onEnable() {
        super.onEnable()
        configManager = ConfigManager(this)
        messageManager = MessageManager(this)
        initCommands()
        this.launchAsync { connect() }
    }

    override fun onDisable() {
        super.onDisable()
        runBlocking {
            disconnect()
        }
    }

    suspend fun connect() {
        try {
            client.connect()
            registerSuspendingEvents(PlayerCountListener(this@DiscordIntegration))
            registerSuspendingEvents(ChatListener(this@DiscordIntegration))
            registerSuspendingEvents(DeathListener(this@DiscordIntegration))
            this@DiscordIntegration.launchAsync {
                client.initListeners()
            }
            activityJob = this@DiscordIntegration.launchAsync {
                while (isActive) {
                    client.updatePlayerCount()
                    delay(configManager.activityUpdateInterval.toLong() * 1000)
                }
            }
            Bukkit.broadcastMessage(messageManager.connected)
        } catch (error: Exception) {
            Bukkit.broadcastMessage(messageManager.failedToConnect)
            error.printStackTrace()
        }
    }

    suspend fun disconnect() {
        activityJob?.cancel()
        activityJob = null
        client.disconnect()
    }

    private fun initCommands() {
        val manager = PaperCommandManager(this)
        manager.registerCommand(DiscordIntegrationCommand(this))
    }

    private suspend fun formatMessage(
        template: String,
        message: Message,
        channel: GuildMessageChannel
    ): String {
        val author = message.author.get()
        val nickname = message.authorAsMember.awaitFirstOrNull()?.displayName
            ?: author.username
        return template
            .replace("%username%", author.username)
            .replace("%user-tag%", author.tag)
            .replace("%nickname%", nickname)
            .replace("%channel-name%", channel.name)
            .replace("%channel-id%", channel.id.asString())
            .replace("%guild-name%", channel.guild.awaitFirst().name)
            .replace("%content%", message.content)
            .trimEnd()
    }

    suspend fun sendDiscordMessage(message: Message) {
        val channel = message.channel.awaitFirstOrNull()
        if (channel == null || channel !is GuildMessageChannel) return
        val messageComponent = TextComponent(formatMessage(
            messageManager.minecraftMessage,
            message,
            channel
        ))
        messageComponent.hoverEvent = HoverEvent(
            HoverEvent.Action.SHOW_TEXT,
            ComponentBuilder(formatMessage(
                messageManager.minecraftTooltip,
                message,
                channel
            )).create()
        )
        server.spigot().broadcast(messageComponent)
    }

    private fun registerSuspendingEvents(listener: Listener) {
        server.pluginManager.registerSuspendingEvents(listener, this)
    }
}
