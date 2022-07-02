package com.dominikkorsa.discordintegration

import com.dominikkorsa.discordintegration.tps.TpsService
import com.dominikkorsa.discordintegration.utils.orNull
import com.dominikkorsa.discordintegration.utils.swapped
import com.google.common.collect.ImmutableMap
import discord4j.common.util.Snowflake
import discord4j.core.DiscordClient
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.guild.EmojisUpdateEvent
import discord4j.core.event.domain.guild.GuildCreateEvent
import discord4j.core.event.domain.guild.GuildDeleteEvent
import discord4j.core.event.domain.guild.MemberJoinEvent
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.command.ApplicationCommandOption
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.GuildEmoji
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Role
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.reactive.*
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
        gateway = client
            .gateway()
            .setEnabledIntents(IntentSet.of(Intent.GUILD_MEMBERS))
            .login()
            .awaitFirstOrNull() ?: throw Exception("Failed to connect to Discord")
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

    private suspend fun initEmojis() {
        gateway?.let { it ->
            val result = HashMap<Snowflake, ImmutableMap<String, String>>()
            it.guilds.collect { result[it.id] = mapEmojis(it.emojis.collectList().awaitFirst()) }
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
                        .collect {
                            guildEmojis?.set(it.guild.id, mapEmojis(it.guild.emojis.collectList().awaitFirst()))
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
                                "link-minecraft" -> handleLinkMinecraftCommand(it)
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

    private suspend fun handleLinkMinecraftCommand(event: ChatInputInteractionEvent) {
        event.deferReply().withEphemeral(true).awaitFirstOrNull()
        val player = plugin.linking.link(
            event.getOption("code").orElseThrow().value.orElseThrow().asString(),
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

    private suspend fun initCommands() {
        gateway?.guilds?.collect {
            registerCommands(it.id)
        }
    }

    private suspend fun registerCommands(guildId: Snowflake) {
        val linkMinecraftCommand = ApplicationCommandRequest.builder()
            .name("link-minecraft")
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

        val commands = if (plugin.configManager.linking.enabled) listOf(linkMinecraftCommand) else listOf()

        gateway?.restClient?.let {
            it.applicationService.bulkOverwriteGuildApplicationCommand(
                it.applicationId.cache().awaitFirst(),
                guildId.asLong(),
                commands
            ).awaitFirstOrNull()
        }
    }

    suspend fun getMember(guildId: Snowflake, userId: Snowflake) =
        gateway?.getMemberById(guildId, userId)?.awaitFirstOrNull()

    suspend fun getRole(guildId: Snowflake, roleId: Snowflake) =
        gateway?.getRoleById(guildId, roleId)?.awaitFirstOrNull()

    suspend fun getChannel(channelId: Snowflake) = gateway?.getChannelById(channelId)?.awaitFirstOrNull()

    private suspend fun getLinkingRoles(guild: Guild, linked: Boolean): MutableList<Role> {
        val roleIds = when {
            linked -> plugin.configManager.linking.linkedRoles
            else -> plugin.configManager.linking.notLinkedRoles
        }
        return guild.roles
            .filter { roleIds.contains(it.id.asString()) }
            .collectList()
            .awaitSingle()
    }

    private suspend fun getLinkingRoles(guild: Guild): Pair<MutableList<Role>, MutableList<Role>> {
        return Pair(getLinkingRoles(guild, true), getLinkingRoles(guild, false))
    }

    private suspend fun updateMember(member: Member, roles: Pair<List<Role>, List<Role>>) {
        if (member.isBot) return
        val player = plugin.linking.playerOfMember(member.id)
        val (addedRoles, removedRoles) = if (player == null) roles.swapped() else roles
        addedRoles.forEach {
            if (!member.roleIds.contains(it.id)) member.addRole(it.id).awaitFirstOrNull()
        }
        removedRoles.forEach {
            if (member.roleIds.contains(it.id)) member.removeRole(it.id).awaitFirstOrNull()
        }
        if (plugin.configManager.linking.syncNicknames) {
            val name = player?.let { Bukkit.getOfflinePlayer(it.id.value) }?.name
            if (member.nickname.orNull() != name) {
                try {
                    member.edit(
                        GuildMemberEditSpec.create().withNicknameOrNull(name)
                    ).awaitFirstOrNull()
                } catch (error: ClientException) {
                    plugin.logger.warning("Cannot change nickname of user @${member.tag} to ${name ?: "null"}, reason:\n${error.message}")
                }
            }
        }
    }

    private suspend fun updateAllMembers() {
        gateway?.guilds?.collect { guild ->
            val roles = getLinkingRoles(guild)
            guild.members.collect { updateMember(it, roles) }
        }
    }

    suspend fun updateMember(memberId: Snowflake) {
        gateway?.guilds?.collect { guild ->
            updateMember(
                guild.getMemberById(memberId).awaitFirstOrNull() ?: return@collect,
                getLinkingRoles(guild)
            )
        }
    }
}
