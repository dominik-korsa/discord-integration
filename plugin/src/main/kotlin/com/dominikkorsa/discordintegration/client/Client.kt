package com.dominikkorsa.discordintegration.client

import com.dominikkorsa.discordintegration.DiscordIntegration
import com.dominikkorsa.discordintegration.exception.MissingIntentsException
import com.dominikkorsa.discordintegration.utils.*
import com.google.common.collect.ImmutableMap
import discord4j.common.close.CloseException
import discord4j.common.store.Store
import discord4j.common.store.impl.LocalStoreLayout
import discord4j.common.util.Snowflake
import discord4j.core.DiscordClient
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.EventDispatcher
import discord4j.core.event.domain.Event
import discord4j.core.event.domain.guild.EmojisUpdateEvent
import discord4j.core.event.domain.guild.GuildCreateEvent
import discord4j.core.event.domain.guild.GuildDeleteEvent
import discord4j.core.event.domain.guild.MemberJoinEvent
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.event.domain.interaction.UserInteractionEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.entity.*
import discord4j.core.`object`.entity.channel.Channel
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import discord4j.core.`object`.entity.channel.TopLevelGuildMessageChannel
import discord4j.core.`object`.presence.ClientActivity
import discord4j.core.`object`.presence.ClientPresence
import discord4j.core.`object`.presence.Status
import discord4j.core.spec.EmbedCreateSpec
import discord4j.core.spec.GuildMemberEditSpec
import discord4j.core.spec.MessageCreateSpec
import discord4j.gateway.intent.Intent
import discord4j.gateway.intent.IntentSet
import discord4j.rest.http.client.ClientException
import discord4j.rest.util.Color
import discord4j.rest.util.Permission
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.bukkit.Bukkit
import reactor.core.CorePublisher
import java.time.Duration
import java.time.LocalDateTime.now


@Suppress("ReactiveStreamsUnusedPublisher")
class Client(private val plugin: DiscordIntegration) {
    companion object {
        private class AddListenersContext(
            private val coroutineScope: CoroutineScope,
            private val eventDispatcher: EventDispatcher,
            private val deferredList: MutableList<Deferred<Unit>>,
        ) {
            fun <T: Event> on(event: Class<T>, listener: suspend (event: T) -> Unit) {
                deferredList.add(coroutineScope.async {
                    eventDispatcher
                        .on(event)
                        .asFlow()
                        .collect {
                            listener(it)
                        }
                })
            }
        }

        private suspend fun GatewayDiscordClient.awaitListeners(callback: AddListenersContext.() -> Unit) = coroutineScope {
            val deferredList: MutableList<Deferred<Unit>> = mutableListOf()
            AddListenersContext(this, eventDispatcher, deferredList).apply(callback)
            deferredList.awaitAll()
        }
    }

    private var gateway: GatewayDiscordClient? = null
    private var guildEmojis: HashMap<Snowflake, ImmutableMap<String, String>>? = null

    suspend fun connect(token: String) {
        val client = DiscordClient.create(token)
        try {
            gateway = client
                .gateway()
                .setStore(Store.fromLayout(LocalStoreLayout.create()))
                .setEnabledIntents(IntentSet.of(Intent.GUILD_MEMBERS, Intent.GUILD_MESSAGES))
                .login()
                .awaitFirstOrNull() ?: throw Exception("Failed to connect to Discord")
        } catch (error: CloseException) {
            if (error.closeStatus.code == 4014) throw MissingIntentsException(client.applicationId.awaitFirst())
            throw error
        }
        initEmojis()
        initCommands()
        updateAllMembers()
    }

    suspend fun disconnect() {
        gateway?.apply {
            logout().awaitFirstOrNull()
            eventDispatcher.shutdown()
        }
        gateway = null
    }

    private suspend fun initEmojis() = coroutineScope {
        gateway?.let { it ->
            val result = HashMap<Snowflake, ImmutableMap<String, String>>()
            it.guilds
                .asFlow()
                .map { async { result[it.id] = mapEmojis(it.emojis.asFlow().toList()) } }
                .toList()
                .awaitAll()
            guildEmojis = result
        }
    }

    private fun mapEmojis(emojis: Collection<GuildEmoji>): ImmutableMap<String, String> {
        val map = mutableMapOf<String, String>()
        emojis.forEach { map[it.name] = it.asFormat() }
        return ImmutableMap.copyOf(map)
    }

    private fun messagesDebug(log: String) {
        if (!plugin.configManager.debug.logDiscordMessages) return
        plugin.logger.info(log)
    }

    suspend fun initListeners() {
        gateway?.apply {
            val allowedConsoleChannels = checkForConsolePermissionIssues()
            awaitListeners {
                on(MessageCreateEvent::class.java) {
                    onMessageCreate(it.message, allowedConsoleChannels)
                }
                on(GuildCreateEvent::class.java) {
                    onGuildCreate(it.guild)
                }
                on(GuildDeleteEvent::class.java) {
                    guildEmojis?.remove(it.guildId)
                }
                on(EmojisUpdateEvent::class.java) {
                    guildEmojis?.set(it.guildId, mapEmojis(it.emojis))
                }
                on(ChatInputInteractionEvent::class.java) {
                    plugin.discordCommands.handleChatInputInteraction(it)
                }
                on(UserInteractionEvent::class.java) {
                    plugin.discordCommands.handleUserInteraction(it)
                }
                on(MemberJoinEvent::class.java) {
                    val roles = getLinkingRoles(it.guild.awaitFirst())
                    updateMember(it.member, roles)
                }
            }
        }
    }

    private suspend fun onMessageCreate(message: Message, allowedConsoleChannels: List<Snowflake>) {
        val chatChannels = plugin.configManager.chat.channels.map(Snowflake::of)
        val consoleChannels = plugin.configManager.chat.consoleChannels.map(Snowflake::of)
        val channelId = message.channelId
        when {
            chatChannels.contains(channelId) -> onSyncedMessage(message)
            consoleChannels.contains(channelId) -> onConsoleMessage(
                message,
                allowedConsoleChannels
            )
            else -> messagesDebug(
                "Ignoring message ${message.id.asString()}, channel ${
                    message.channelId.asString()
                } not configured in chat.channels or chat.console-channels"
            )
        }
    }

    private suspend fun onGuildCreate(guild: Guild) = coroutineScope {
        guildEmojis?.set(guild.id, mapEmojis(guild.emojis.collectList().awaitFirst()))
        val roles = getLinkingRoles(guild)
        guild.members
            .asFlow()
            .mapConcurrently { member -> updateMember(member, roles) }
        registerCommands(guild.id)
    }

    private suspend fun onSyncedMessage(message: Message) {
        messagesDebug("Received message ${message.id.asString()} on channel ${message.channelId.asString()}")
        when {
            !message.author.isPresent -> messagesDebug("Ignoring message, cannot get message author")
            message.author.get().isBot -> messagesDebug("Ignoring message, author is a bot")
            message.content.isNullOrEmpty() -> messagesDebug("Ignoring message, content empty")
            else -> {
                val timeStart = now()
                plugin.broadcastDiscordMessage(message)
                messagesDebug(
                    "Processing chat message took ${
                        Duration.between(timeStart, now()).toMillis()
                    } milliseconds"
                )
            }
        }
    }

    private suspend fun onConsoleMessage(message: Message, allowedConsoleChannels: List<Snowflake>) {
        if (message.author.orNull()?.isBot != false) return
        if (message.content.isEmpty()) return
        if (!allowedConsoleChannels.contains(message.channelId)) {
            message.channel.awaitFirstOrNull()?.let {
                it.createMessage(
                    MessageCreateSpec.create()
                        .withEmbeds(getExecutionDisabledSpec(it))
                        .withMessageReference(message.id)
                )
            }?.awaitFirstOrNull()
            return
        }
        plugin.runConsoleCommand(message.content)
    }

    suspend fun updateActivity() {
        gateway?.apply {
            val players = Bukkit.getOnlinePlayers()
            val message = plugin.discordFormatter.formatActivity(
                players,
                Bukkit.getMaxPlayers(),
                (Bukkit.getWorld(plugin.configManager.activity.timeWorld) ?: Bukkit.getWorlds()[0]).time
            )
            val status = if (players.isEmpty() && plugin.configManager.activity.idle) Status.IDLE else Status.ONLINE
            updatePresence(ClientPresence.of(status, ClientActivity.playing(message))).awaitFirstOrNull()
        }
    }

    suspend fun getWebhooks() = gateway?.let { gateway ->
        plugin.configManager.chat.webhooks.asFlow().map {
            Webhooks.parseWebhookUrl(it)?.let { (id, token) ->
                gateway.getWebhookByIdWithToken(id, token).awaitFirstOrNull()
            }
        }.filterNotNull()
    }

    fun getEmojiFormat(name: String) = guildEmojis?.firstNotNullOfOrNull { it.value[name] }

    private suspend fun initCommands(): Unit = coroutineScope {
        gateway?.guilds?.asFlow()?.map {
            async {
                registerCommands(it.id)
            }
        }?.toList()?.awaitAll()
    }

    private suspend fun registerCommands(guildId: Snowflake) {
        val commands = plugin.discordCommands.createCommands()

        gateway?.restClient?.let {
            it.applicationService.bulkOverwriteGuildApplicationCommand(
                it.applicationId.cache().awaitFirst(),
                guildId.asLong(),
                commands
            ).awaitFirstOrNull()
        }
    }

    private suspend fun <T> CorePublisher<T>.handleNotFound(): T? {
        try {
            return awaitFirstOrNull()
        } catch (exception: ClientException) {
            if (exception.status.code() == 404) return null
            throw exception
        }
    }

    suspend fun getMember(guildId: Snowflake, userId: Snowflake) =
        gateway?.getMemberById(guildId, userId)?.handleNotFound()

    suspend fun getRole(guildId: Snowflake, roleId: Snowflake) =
        gateway?.getRoleById(guildId, roleId)?.handleNotFound()

    suspend fun getChannel(channelId: Snowflake) =
        gateway?.getChannelById(channelId)?.handleNotFound()

    private suspend fun getLinkingRoles(guild: Guild, linked: Boolean): List<Role> {
        val roleIds = when {
            linked -> plugin.configManager.linking.linkedRoles
            else -> plugin.configManager.linking.notLinkedRoles
        }
        return guild.roles
            .asFlow()
            .filter { roleIds.contains(it.id.asString()) }
            .toList()
    }

    private suspend fun getLinkingRoles(guild: Guild): Pair<List<Role>, List<Role>> = coroutineScope {
        val (linked, notLinked) = awaitAll(
            async { getLinkingRoles(guild, true) },
            async { getLinkingRoles(guild, false) },
        )
        Pair(linked, notLinked)
    }

    private suspend fun updateMember(member: Member, roles: Pair<List<Role>, List<Role>>) = coroutineScope {
        if (member.isBot) return@coroutineScope
        val playerId = plugin.db.playerIdOfMember(member.id)
        val (addedRoles, removedRoles) = if (playerId == null) roles.swapped() else roles
        var showWarning = false
        awaitAll(async {
            addedRoles.forEach {
                try {
                    if (!member.roleIds.contains(it.id)) member.addRole(it.id).awaitFirstOrNull()
                } catch (error: ClientException) {
                    plugin.logger.warning("Cannot add role `${it.name}` to user @${member.tag}, reason:\n${error.message}")
                    showWarning = true
                }
            }
        }, async {
            removedRoles.forEach {
                try {
                    if (member.roleIds.contains(it.id)) member.removeRole(it.id).awaitFirstOrNull()
                } catch (error: ClientException) {
                    plugin.logger.warning("Cannot remove role `${it.name}` from user @${member.tag}, reason:\n${error.message}")
                    showWarning = true
                }
            }
        }, async {
            if (!plugin.configManager.linking.enabled || !plugin.configManager.linking.syncNicknames) return@async
            val name = playerId?.let(Bukkit::getOfflinePlayer)?.name
            if (member.nickname.orNull() == name) return@async
            try {
                member.edit(
                    GuildMemberEditSpec.create().withNicknameOrNull(name)
                ).awaitFirstOrNull()
            } catch (error: ClientException) {
                plugin.logger.warning("Cannot change nickname of user @${member.tag} to ${name ?: "(none)"}, reason:\n${error.message}")
                showWarning = true
            }
        })
        if (showWarning) plugin.logger.warning("Make sure to put the bot's role on the top of the role list")
    }

    private suspend fun updateAllMembers(): Unit = coroutineScope {
        gateway
            ?.guilds
            ?.asFlow()
            ?.map { guild ->
                async {
                    val roles = getLinkingRoles(guild)
                    guild
                        .members
                        .asFlow()
                        .map { async { updateMember(it, roles) } }
                        .toList()
                        .awaitAll()
                }
            }
            ?.toList()
            ?.awaitAll()
    }

    suspend fun updateMember(memberId: Snowflake): Unit = coroutineScope {
        gateway
            ?.guilds
            ?.asFlow()
            ?.map { guild ->
                async {
                    updateMember(
                        guild.getMemberById(memberId).handleNotFound() ?: return@async,
                        getLinkingRoles(guild)
                    )
                }
            }
            ?.toList()
            ?.awaitAll()
    }

    suspend fun sendConsoleMessage(message: String): Unit = coroutineScope {
        gateway?.apply {
            plugin.configManager.chat.consoleChannels.map { channelId ->
                async {
                    getChannelById(Snowflake.of(channelId))
                        .awaitFirstOrNull<Channel?>()
                        ?.tryCast<GuildMessageChannel>()
                        ?.createMessage(message)
                        ?.awaitFirstOrNull()
                }
            }.awaitAll()
        }
    }

    private fun getExecutionDisabledSpec(channel: Channel) = EmbedCreateSpec.create()
        .withColor(Color.of(0xef476f))
        .withTitle("Command execution disabled")
        .withDescription(
            """
                Console channel <#${channel.id.asString()}> allows @everyone to send messages.
                
                To prevent unauthorized access, the ability to execute commands has been disabled.
                
                Please remove the `Send messages` or `View channel` permission from the @everyone role in the console channel and run `/di reload`
            """.trimIndent()
        )

    private suspend fun checkForConsolePermissionIssues(): List<Snowflake> = coroutineScope {
        gateway?.run {
            plugin.configManager.chat.consoleChannels
                .map(Snowflake::of)
                .map { id ->
                    id to async {
                        val channel = getChannelById(id).awaitSingleOrNull()?.tryCast<GuildMessageChannel>()
                        if (channel == null) {
                            plugin.logger.severe("Console channel with id ${id.asString()} not found")
                            return@async false
                        }
                        if (channel !is TopLevelGuildMessageChannel) {
                            plugin.logger.severe("Thread \"${channel.name}\" with id ${id.asString()} cannot be used as a console channel")
                            return@async false
                        }
                        val permissions = channel.getEffectiveEveryonePermissions()
                        if (permissions.contains(Permission.SEND_MESSAGES) && permissions.contains(Permission.VIEW_CHANNEL)) {
                            """
                                Console channel #${channel.name} with id ${id.asString()}
                                allows @everyone to send messages.
                                
                                To prevent unauthorized access,
                                the ability to execute commands has been disabled.
                                
                                Please remove the Send messages or View channel permission
                                from the @everyone role in the console channel
                                and run /di reload
                            """.trimIndent().lines().forEach(plugin.logger::severe)
                            channel.createMessage(getExecutionDisabledSpec(channel)).awaitFirstOrNull()
                            return@async false
                        }
                        return@async true
                    }
                }
                .filter { it.second.await() }
                .map { it.first }
        } ?: emptyList()
    }
}
