package com.dominikkorsa.discordintegration

import com.github.shynixn.mccoroutine.launchAsync
import com.github.shynixn.mccoroutine.registerSuspendingEvents
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.hover.content.Content
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DiscordIntegration: JavaPlugin() {
    lateinit var client: Client
    lateinit var configManager: ConfigManager

    override fun onEnable() {
        super.onEnable()
        saveDefaultConfig()
        configManager = ConfigManager(this)
        client = Client(this)
        this.launchAsync {
            client.main()
            server.pluginManager.registerSuspendingEvents(
                PlayerCountListener(this@DiscordIntegration),
                this@DiscordIntegration
            )
            server.pluginManager.registerSuspendingEvents(
                ChatListener(this@DiscordIntegration),
                this@DiscordIntegration
            )
            this@DiscordIntegration.launchAsync {
                client.initListeners()
            }
            Bukkit.broadcastMessage("§9§lDiscord Integration connected")
        }
    }

    override fun onDisable() {
        super.onDisable()
        runBlocking {
            client.disconnect()
        }
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
            configManager.chatMinecraftMessage,
            message,
            channel
        ))
        messageComponent.hoverEvent = HoverEvent(
            HoverEvent.Action.SHOW_TEXT,
            ComponentBuilder(formatMessage(
                configManager.chatMinecraftTooltip,
                message,
                channel
            )).create()
        )
        server.spigot().broadcast(messageComponent)
    }
}
