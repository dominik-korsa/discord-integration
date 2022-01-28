package com.dominikkorsa.discordintegration

import com.dominikkorsa.discordintegration.tps.TpsService
import discord4j.common.util.Snowflake
import discord4j.core.DiscordClient
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Webhook
import discord4j.core.`object`.presence.ClientActivity
import discord4j.core.`object`.presence.ClientPresence
import discord4j.core.`object`.presence.Status
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.WebhookExecuteSpec
import discord4j.rest.util.AllowedMentions
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bukkit.Bukkit
import org.bukkit.entity.Player

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
            val status = if (players.isEmpty() && plugin.configManager.activityIdle) Status.IDLE else Status.ONLINE
            updatePresence(ClientPresence.of(status, ClientActivity.playing(message))).awaitFirstOrNull()
        }
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

    fun getWebhookBuilder(): WebhookExecuteSpec.Builder {
        return WebhookExecuteSpec.builder()
            .allowedMentions(allowedMentionsNone)
    }

    suspend fun getPlayerWebhookBuilder(player: Player): WebhookExecuteSpec.Builder {
        return getWebhookBuilder()
            .username(player.name)
            .avatarUrl(plugin.avatarService.getAvatarUrl(player))
    }

    suspend fun sendWebhook(spec: WebhookExecuteSpec) {
        getWebhooks()?.forEach {
            it.execute(spec).awaitFirstOrNull()
        }
    }
}
