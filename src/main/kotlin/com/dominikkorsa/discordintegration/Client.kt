package com.dominikkorsa.discordintegration

import com.dominikkorsa.discordintegration.tps.TpsService
import discord4j.common.util.Snowflake
import discord4j.core.DiscordClient
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.channel.TextChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.MessageCreateSpec
import discord4j.discordjson.json.ImmutableActivityUpdateRequest
import discord4j.discordjson.json.gateway.ImmutableStatusUpdate
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.text.DecimalFormat

class Client(private val plugin: DiscordIntegration) {
    private lateinit var client: DiscordClient
    private var gateway: GatewayDiscordClient? = null
    private val tpsService = TpsService()

    suspend fun main() {
        client = DiscordClient.create(plugin.configManager.discordToken)
        gateway = client.login().awaitFirstOrNull() ?: throw Exception("Failed to connect to Discord")
    }

    suspend fun initListeners() {
        gateway.apply {
            if (this == null) throw Exception("Gateway is null")
            eventDispatcher
                .on(MessageCreateEvent::class.java)
                .asFlow()
                .filter { plugin.configManager.chatChannels.contains(it.message.channelId.asString()) }
                .filterNot { it.message.content.isNullOrEmpty() }
                .filter { it.message.author.isPresent }
                .filterNot { it.message.author.get().isBot }
                .collect {
                    plugin.sendDiscordMessage(it.message)
                }
        }
    }

    suspend fun updatePlayerCount() {
        gateway?.apply {
            val players = Bukkit.getOnlinePlayers()
            val messageTemplate = when {
                players.isNotEmpty() -> plugin.messageManager.discordActivity
                else -> plugin.messageManager.discordActivityEmpty
            }
            val tps = tpsService.getRecentTps()
            val df = DecimalFormat("#0.00")
            val message = messageTemplate
                .replace("%online%", players.size.toString())
                .replace("%max%", Bukkit.getMaxPlayers().toString())
                .replace("%player-list%", players
                    .map { player -> player.name }
                    .sorted()
                    .joinToString(", "))
                .replace("%tps-1m%", df.format(tps.of1min))
                .replace("%tps-5m%", df.format(tps.of5min))
                .replace("%tps-15m%", df.format(tps.of15min))

            val statusUpdateBuilder = ImmutableStatusUpdate.builder()
            statusUpdateBuilder.afk(false)
            val activityUpdateBuilder = ImmutableActivityUpdateRequest.builder()
            activityUpdateBuilder.type(0)
            activityUpdateBuilder.name(message)
            statusUpdateBuilder.activities(listOf(activityUpdateBuilder.build()))
            statusUpdateBuilder.status("available")
            updatePresence(statusUpdateBuilder.build()).awaitFirstOrNull()
        }
    }

    private suspend fun getChatChannel(id: Snowflake): TextChannel {
        val channel = gateway
            ?.getChannelById(id)
            ?.awaitFirstOrNull() ?: throw Exception("Channel not found")
        if (channel !is TextChannel) throw Exception("Channel is not of type Text")
        return channel
    }

    private suspend fun getChatChannel(id: String): TextChannel {
        return getChatChannel(Snowflake.of(id))
    }

    private suspend fun sendMessage(function: (spec: MessageCreateSpec) -> Unit) {
        plugin.configManager.chatChannels
            .map { getChatChannel(it) }
            .forEach {
                it.createMessage { spec ->
                    function(spec)
                }.awaitFirstOrNull()
            }
    }

    suspend fun sendChatMessage(
        playerName: String,
        avatarUrl: String,
        content: String
    ) {
        gateway?.let { gateway ->
            plugin.configManager.chatWebhooks
                .mapNotNull {
                    Regex("/api/webhooks/([^/]+)/([^/]+)\$").find(it)?.let { result ->
                        gateway
                            .getWebhookByIdWithToken(
                                Snowflake.of(result.groupValues[1]),
                                result.groupValues[2],
                            )
                            .awaitFirstOrNull()
                    }
                }
                .forEach {
                    it.execute { spec ->
                        spec.setUsername(playerName)
                        spec.setAvatarUrl(avatarUrl)
                        spec.setContent(content)
                    }.awaitFirstOrNull()
                }
        }
    }

    suspend fun sendJoinInfo(player: Player) {
        val content = plugin.messageManager.discordJoin
            .replace("%player%", player.name)
        sendMessage { it.setContent(content) }
    }

    suspend fun sendQuitInfo(player: Player) {
        val content = plugin.messageManager.discordQuit
            .replace("%player%", player.name)
        sendMessage { it.setContent(content) }
    }

    suspend fun disconnect() {
        gateway?.logout()?.awaitFirstOrNull()
    }

    suspend fun sendDeathInfo(deathMessage: String) {
        sendMessage { it.setContent(deathMessage) }
    }
}
