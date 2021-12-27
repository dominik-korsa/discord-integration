package com.dominikkorsa.discordintegration

import com.dominikkorsa.discordintegration.tps.TpsService
import discord4j.common.util.Snowflake
import discord4j.core.DiscordClient
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Webhook
import discord4j.core.`object`.entity.channel.TextChannel
import discord4j.core.`object`.presence.ClientActivity
import discord4j.core.`object`.presence.ClientPresence
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.legacy.LegacyWebhookExecuteSpec
import discord4j.rest.util.AllowedMentions
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bukkit.Bukkit

class Client(private val plugin: DiscordIntegration) {
    companion object {
        private val allowedMentionsNone = AllowedMentions.builder().build()
    }

    private var gateway: GatewayDiscordClient? = null
    private val tpsService = TpsService()

    suspend fun connect() {
        val client = DiscordClient.create(plugin.configManager.discordToken)
        gateway = client.login().awaitFirstOrNull() ?: throw Exception("Failed to connect to Discord")
    }

    suspend fun disconnect() {
        gateway?.apply {
            logout().awaitFirstOrNull()
            eventDispatcher.shutdown()
        }
        gateway = null
    }

    suspend fun initListeners() {
        gateway?.apply {
            eventDispatcher
                .on(MessageCreateEvent::class.java)
                .asFlow()
                .filter { plugin.configManager.chatChannels.contains(it.message.channelId.asString()) }
                .filterNot { it.message.content.isNullOrEmpty() }
                .filter { it.message.author.isPresent }
                .filterNot { it.message.author.get().isBot }
                .collect {
                    plugin.broadcastDiscordMessage(it.message)
                }
        }
    }

    suspend fun updateActivity() {
        gateway?.apply {
            val players = Bukkit.getOnlinePlayers()
            val tps = tpsService.getRecentTps()
            val message = plugin.discordFormatter.formatActivity(
                players,
                Bukkit.getMaxPlayers(),
                tps,
                (Bukkit.getWorld(plugin.configManager.activityTimeWorld) ?: Bukkit.getWorlds()[0]).time
            )

            updatePresence(ClientPresence.online(ClientActivity.playing(message))).awaitFirstOrNull()
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

    private suspend fun getWebhooks(): Collection<Webhook>? {
        return gateway?.let { gateway ->
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
        }
    }

    suspend fun sendWebhook(function: (spec: LegacyWebhookExecuteSpec) -> Unit) {
        getWebhooks()?.forEach {
            it.execute { spec ->
                function(spec)
                spec.setAllowedMentions(allowedMentionsNone)
            }.awaitFirstOrNull()
        }
    }

    suspend fun sendWebhook(content: String) {
        sendWebhook { it.setContent(content) }
    }

    suspend fun sendChatMessage(
        playerName: String,
        avatarUrl: String,
        content: String
    ) {
        sendWebhook {
            it.setUsername(playerName)
            it.setAvatarUrl(avatarUrl)
            it.setContent(content)
        }
    }
}
