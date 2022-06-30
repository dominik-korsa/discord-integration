package com.dominikkorsa.discordintegration

import com.dominikkorsa.discordintegration.tps.TpsService
import com.google.common.collect.ImmutableMap
import discord4j.common.util.Snowflake
import discord4j.core.DiscordClient
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.guild.EmojisUpdateEvent
import discord4j.core.event.domain.guild.GuildCreateEvent
import discord4j.core.event.domain.guild.GuildDeleteEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.entity.GuildEmoji
import discord4j.core.`object`.presence.ClientActivity
import discord4j.core.`object`.presence.ClientPresence
import discord4j.core.`object`.presence.Status
import discord4j.core.spec.WebhookExecuteSpec
import discord4j.rest.util.AllowedMentions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.collect
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class Client(private val plugin: DiscordIntegration) {
    companion object {
        private val allowedMentionsNone = AllowedMentions.builder().build()
    }

    private var gateway: GatewayDiscordClient? = null
    private val tpsService = TpsService()
    private var guildEmojis: HashMap<Snowflake, ImmutableMap<String, String>>? = null

    suspend fun connect() {
        val client = DiscordClient.create(plugin.configManager.discordToken)
        gateway = client.login().awaitFirstOrNull() ?: throw Exception("Failed to connect to Discord")
        initEmojis()
    }

    suspend fun disconnect() {
        gateway?.apply {
            logout().awaitFirstOrNull()
            eventDispatcher.shutdown()
        }
        gateway = null
    }

    private suspend fun initEmojis() {
        gateway?.apply {
            val result = HashMap<Snowflake, ImmutableMap<String, String>>()
            guilds.collect { result[it.id] = mapEmojis(it.emojis.collectList().awaitFirst()) }
            guildEmojis = result
        }
    }

    private fun mapEmojis(emojis: Collection<GuildEmoji>): ImmutableMap<String, String> {
        val map = mutableMapOf<String, String>()
        emojis.forEach { map[it.name] = it.asFormat() }
        return ImmutableMap.copyOf(map)
    }

    suspend fun initListeners() = coroutineScope {
        gateway?.apply {
            awaitAll(
                async {
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
                },
                async {
                    eventDispatcher.on(GuildCreateEvent::class.java)
                        .collect { guildEmojis?.set(it.guild.id, mapEmojis(it.guild.emojis.collectList().awaitFirst())) }
                },
                async {
                    eventDispatcher.on(GuildDeleteEvent::class.java)
                        .collect { guildEmojis?.remove(it.guildId) }
                },
                async {
                    eventDispatcher.on(EmojisUpdateEvent::class.java)
                        .collect { guildEmojis?.set(it.guildId, mapEmojis(it.emojis)) }
                },
            )
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

    private suspend fun getWebhooks() = gateway?.let { gateway ->
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

    fun getWebhookBuilder(): WebhookExecuteSpec.Builder = WebhookExecuteSpec.builder()
        .allowedMentions(allowedMentionsNone)

    suspend fun getPlayerWebhookBuilder(player: Player): WebhookExecuteSpec.Builder = getWebhookBuilder()
        .username(player.name)
        .avatarUrl(plugin.avatarService.getAvatarUrl(player))

    suspend fun sendWebhook(spec: WebhookExecuteSpec) {
        getWebhooks()?.forEach {
            it.execute(spec).awaitFirstOrNull()
        }
    }

    fun getEmojiFormat(name: String) = guildEmojis?.firstNotNullOfOrNull { it.value[name] }

    suspend fun getMember(guildId: Snowflake, userId: Snowflake) = gateway?.getMemberById(guildId, userId)?.awaitFirstOrNull()

    suspend fun getRole(guildId: Snowflake, roleId: Snowflake) = gateway?.getRoleById(guildId, roleId)?.awaitFirstOrNull()

    suspend fun getChannel(channelId: Snowflake) = gateway?.getChannelById(channelId)?.awaitFirstOrNull()
}
