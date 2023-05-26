package com.dominikkorsa.discordintegration

/* imports for imagemaps */

import com.dominikkorsa.discordintegration.exception.MissingIntentsException
import com.dominikkorsa.discordintegration.utils.getEffectiveEveryonePermissions
import com.dominikkorsa.discordintegration.utils.orNull
import com.dominikkorsa.discordintegration.utils.swapped
import com.dominikkorsa.discordintegration.utils.tryCast
import com.google.common.collect.ImmutableMap
import discord4j.common.close.CloseException
import discord4j.common.store.Store
import discord4j.common.store.impl.LocalStoreLayout
import discord4j.common.util.Snowflake
import discord4j.core.DiscordClient
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.guild.EmojisUpdateEvent
import discord4j.core.event.domain.guild.GuildCreateEvent
import discord4j.core.event.domain.guild.GuildDeleteEvent
import discord4j.core.event.domain.guild.MemberJoinEvent
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.event.domain.interaction.UserInteractionEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.command.ApplicationCommandOption
import discord4j.core.`object`.entity.*
import discord4j.core.`object`.entity.channel.Channel
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import discord4j.core.`object`.presence.ClientActivity
import discord4j.core.`object`.presence.ClientPresence
import discord4j.core.`object`.presence.Status
import discord4j.core.spec.*
import discord4j.discordjson.json.ApplicationCommandOptionData
import discord4j.discordjson.json.ApplicationCommandRequest
import discord4j.gateway.intent.Intent
import discord4j.gateway.intent.IntentSet
import discord4j.rest.http.client.ClientException
import discord4j.rest.util.AllowedMentions
import discord4j.rest.util.Color
import discord4j.rest.util.Permission
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.collect
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import reactor.core.CorePublisher
import java.net.URL
import java.nio.file.Path
import java.time.Duration
import java.time.LocalDateTime.now
import kotlin.io.path.outputStream



@Suppress("ReactiveStreamsUnusedPublisher")
class Client(private val plugin: DiscordIntegration) {
    companion object {
        private val allowedMentionsNone = AllowedMentions.builder().build()

        private const val linkCommandName = "link-minecraft"
        private const val profileInfoCommandName = "Minecraft profile info"
    }

    private var gateway: GatewayDiscordClient? = null
    private var guildEmojis: HashMap<Snowflake, ImmutableMap<String, String>>? = null
    /* this function downloads the file from the URL provided in the attachments under discords API
    * it saves the file to a temporary file that is deleted regardless if it's an image or not
    * */
    private fun downloadFile(url: URL): Path {
        val file = kotlin.io.path.createTempFile()
        url.openStream().use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

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

    suspend fun initListeners() = coroutineScope {
        gateway?.apply {
            val allowedConsoleChannels = checkForConsolePermissionIssues()
            awaitAll(
                async {
                    eventDispatcher
                        .on(MessageCreateEvent::class.java)
                        .asFlow()
                        .collect {
                            val chatChannels = plugin.configManager.chat.channels.map(Snowflake::of)
                            val consoleChannels = plugin.configManager.chat.consoleChannels.map(Snowflake::of)
                            val channelId = it.message.channelId
                            when {
                                chatChannels.contains(channelId) -> onSyncedMessage(it.message)
                                consoleChannels.contains(channelId) -> onConsoleMessage(
                                    it.message,
                                    allowedConsoleChannels
                                )
                                else -> messagesDebug(
                                    "Ignoring message ${it.message.id.asString()}, channel ${
                                        it.message.channelId.asString()
                                    } not configured in chat.channels or chat.console-channels"
                                )
                            }
                        }
                },
                async {
                    eventDispatcher.on(GuildCreateEvent::class.java)
                        .collect {
                            guildEmojis?.set(it.guild.id, mapEmojis(it.guild.emojis.collectList().awaitFirst()))
                            val roles = getLinkingRoles(it.guild)
                            it.guild.members
                                .asFlow()
                                .map { member -> async { updateMember(member, roles) } }
                                .toList()
                                .awaitAll()
                            registerCommands(it.guild.id)
                        }
                },
                async {
                    eventDispatcher.on(GuildDeleteEvent::class.java)
                        .collect { guildEmojis?.remove(it.guildId) }
                },
                async {
                    eventDispatcher.on(EmojisUpdateEvent::class.java)
                        .collect { guildEmojis?.set(it.guildId, mapEmojis(it.emojis)) }
                },
                async {
                    eventDispatcher.on(ChatInputInteractionEvent::class.java)
                        .collect {
                            when (it.commandName) {
                                linkCommandName -> handleLinkMinecraftCommand(it)
                                else -> it.deleteReply().awaitFirstOrNull()
                            }
                        }
                },
                async {
                    eventDispatcher.on(UserInteractionEvent::class.java)
                        .collect {
                            when (it.commandName) {
                                profileInfoCommandName -> handleProfileInfoCommand(it)
                                else -> it.deleteReply().awaitFirstOrNull()
                            }
                        }
                },
                async {
                    eventDispatcher.on(MemberJoinEvent::class.java)
                        .collect {
                            val roles = getLinkingRoles(it.guild.awaitFirst())
                            updateMember(it.member, roles)
                        }
                }
            )
        }
    }

    /* listening in for discord messages at a channel with webhooks */
    private suspend fun onSyncedMessage(message: Message) {
        messagesDebug("Received message ${message.id.asString()} on channel ${message.channelId.asString()}")
        when {
            !message.author.isPresent -> messagesDebug("Ignoring message, cannot get message author")
            message.author.get().isBot -> messagesDebug("Ignoring message, author is a bot")
            message.content.isNullOrEmpty() && message.attachments.isEmpty() -> messagesDebug("Ignoring message, content empty")
            /* Here is where we'll check for messages containing files */
            message.attachments.isNotEmpty() -> {
                // verify imageMap integration is enabled
                if (!plugin.configManager.imagemaps.enabled) {
                    messagesDebug("Ignoring attachments, imagemaps integration not enabled")
                    return
                }
                /* verify this message was sent in a imchannel */
                if (!plugin.configManager.imagemaps.channels.map(Snowflake::of).contains(message.channelId)) {
                    messagesDebug("Ignoring attachment, not part of one of the imagemap channels")
                    return
                }

                // communicate that we're processing players file
                if (plugin.configManager.debug.imagemaps) {
                    message.channel.awaitFirstOrNull()?.let {
                        it.createMessage(
                            "Processing attachments %s".format(message.author.get().mention)
                        ).withAllowedMentions(AllowedMentions.builder().allowUser(message.author.get().id).build())
                    }?.awaitFirstOrNull()
                }


                /* Go through each attachment, download and scan for image */
                for (attachment in message.attachments) {
                    /* gets the URL of the attachment for download */
                    var url = attachment.url
                    var filename = attachment.filename.lowercase().replace(" ", "_")
                    var pathToFile = downloadFile(URL(url))

                    /* if not a PNG file then we stop execution */
                    if (!plugin.fileScanner.scan(pathToFile)) {
                        // communicate with player that this attachement is not a PNG
                        if (plugin.configManager.debug.imagemaps) {
                            message.channel.awaitFirstOrNull()?.let {
                                it.createMessage(
                                    "This is not a PNG image! %s %s".format(message.author.get().mention, filename)
                                ).withAllowedMentions(AllowedMentions.builder().allowUser(message.author.get().id).build())
                            }?.awaitFirstOrNull()
                        }
                        messagesDebug("This is not a PNG file!")
                        return
                    }

                    /* if no issue with file, attempt migration */
                    if(!plugin.imageMapMigrator
                        .migrateImage(pathToFile, filename, plugin.configManager.imagemaps.path)) {
                        message.channel.awaitFirstOrNull()?.let {
                            it.
                            createMessage(
                                "Upload failed for file: %s %s".format(filename, message.author.get().mention)
                            ).withAllowedMentions(AllowedMentions.builder().allowUser(message.author.get().id).build())
                        }?.awaitFirstOrNull()
                    }
                    else {
                        // communicate upload complete, use /imagemaps place <filename>
                        if (plugin.configManager.debug.imagemaps) {
                            message.channel.awaitFirstOrNull()?.let {
                                it.createMessage(
                                    "Upload complete, you may use '/imagemap place %s' to place your image %s".format(
                                        filename,
                                        message.author.get().mention)
                                ).withAllowedMentions(AllowedMentions.builder().allowUser(message.author.get().id).build())
                            }?.awaitFirstOrNull()
                        }
                    }

                }


            }
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

    private suspend fun handleLinkMinecraftCommand(event: ChatInputInteractionEvent) {
        event.deferReply().withEphemeral(true).awaitFirstOrNull()
        val player = plugin.linking.link(
            event.getOption("code").get().value.get().asString(),
            event.interaction.user
        )

        if (player == null) {
            event.editReply(
                InteractionReplyEditSpec.create()
                    .withEmbeds(
                        EmbedCreateSpec.create()
                            .withTitle(plugin.messages.discord.linkingUnknownCodeTitle)
                            .withDescription(plugin.messages.discord.linkingUnknownCodeContent)
                            .withColor(Color.of(0xef476f))
                    )
            ).awaitFirstOrNull()
        } else {
            event.editReply(
                InteractionReplyEditSpec.create()
                    .withEmbeds(
                        EmbedCreateSpec.create()
                            .withTitle(plugin.messages.discord.linkingSuccessTitle)
                            .withThumbnail(plugin.avatarService.getAvatarUrl(player))
                            .withFields(
                                EmbedCreateFields.Field.of(
                                    plugin.messages.discord.linkingSuccessPlayerNameHeader,
                                    player.name,
                                    false
                                )
                            )
                            .withColor(Color.of(0x06d6a0))
                    )
            ).awaitFirstOrNull()
        }
    }

    private suspend fun handleProfileInfoCommand(event: UserInteractionEvent) {
        event.deferReply().withEphemeral(true).awaitFirstOrNull()
        val playerId = plugin.db.playerIdOfMember(event.targetId)
        if (playerId == null) {
            event.editReply(
                InteractionReplyEditSpec.create()
                    .withEmbeds(
                        EmbedCreateSpec.create()
                            .withTitle(plugin.messages.discord.profileInfoNotLinked)
                            .withColor(Color.of(0xef476f))
                    )
            ).awaitFirstOrNull()
            return
        }

        val player = Bukkit.getOfflinePlayer(playerId)
        val name = player.name
        if (name == null) {
            event.editReply(
                InteractionReplyEditSpec.create()
                    .withEmbeds(
                        EmbedCreateSpec.create()
                            .withTitle(plugin.messages.discord.profileInfoError)
                            .withColor(Color.of(0xef476f))
                    )
            ).awaitFirstOrNull()
            return
        }

        event.editReply(
            InteractionReplyEditSpec.create()
                .withEmbeds(
                    EmbedCreateSpec.create()
                        .withTitle(plugin.messages.discord.profileInfoTitle)
                        .withFields(
                            EmbedCreateFields.Field.of(
                                plugin.messages.discord.profileInfoPlayerNameHeader,
                                name,
                                false
                            )
                        )
                        .withThumbnail(plugin.avatarService.getAvatarUrl(playerId, name))
                        .withColor(Color.of(0x06d6a0))
                )
        ).awaitFirstOrNull()
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

    private val webhookRegex = Regex("/api/webhooks/([^/]+)/([^/]+)\$")

    private suspend fun getWebhooks() = gateway?.let { gateway ->
        plugin.configManager.chat.webhooks.asFlow().map {
            webhookRegex.find(it)?.let { result ->
                gateway.getWebhookByIdWithToken(
                    Snowflake.of(result.groupValues[1]),
                    result.groupValues[2],
                ).awaitFirstOrNull()
            }
        }.filterNotNull()
    }

    fun getWebhookBuilder(): WebhookExecuteSpec.Builder = WebhookExecuteSpec.builder()
        .allowedMentions(allowedMentionsNone)

    suspend fun getPlayerWebhookBuilder(player: Player): WebhookExecuteSpec.Builder = getWebhookBuilder()
        .username(player.name)
        .avatarUrl(plugin.avatarService.getAvatarUrl(player))

    suspend fun sendWebhook(spec: WebhookExecuteSpec) = coroutineScope {
        getWebhooks()?.map {
            async {
                it.execute(spec).awaitFirstOrNull()
            }
        }?.toList()?.awaitAll()
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
        val linkMinecraftCommand = ApplicationCommandRequest.builder()
            .name(linkCommandName)
            .description("Link Minecraft account to your Discord account")
            .addOption(
                ApplicationCommandOptionData.builder()
                    .name("code")
                    .description("One-time code")
                    .type(ApplicationCommandOption.Type.STRING.value)
                    .required(true)
                    .build()
            )
            .build()

        val userInfoCommand = ApplicationCommandRequest.builder()
            .type(2)
            .name(profileInfoCommandName)
            .build()

        val commands =
            if (plugin.configManager.linking.enabled) listOf(linkMinecraftCommand, userInfoCommand) else listOf()

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
