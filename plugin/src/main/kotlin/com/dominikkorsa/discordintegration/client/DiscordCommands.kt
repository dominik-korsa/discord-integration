package com.dominikkorsa.discordintegration.client

import com.dominikkorsa.discordintegration.DiscordIntegration
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.event.domain.interaction.UserInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandOption
import discord4j.core.spec.EmbedCreateFields
import discord4j.core.spec.EmbedCreateSpec
import discord4j.core.spec.InteractionReplyEditSpec
import discord4j.discordjson.json.ApplicationCommandOptionData
import discord4j.discordjson.json.ApplicationCommandRequest
import discord4j.discordjson.json.ImmutableApplicationCommandRequest
import discord4j.rest.util.Color
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bukkit.Bukkit

class DiscordCommands(private val plugin: DiscordIntegration) {
    companion object {
        private const val linkCommandName = "link-minecraft"
        private const val profileInfoCommandName = "Minecraft profile info"
    }

    suspend fun handleChatInputInteraction(event: ChatInputInteractionEvent) {
        when (event.commandName) {
            linkCommandName -> handleLinkMinecraftCommand(event)
            else -> event.deleteReply().awaitFirstOrNull()
        }
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

    suspend fun handleUserInteraction(event: UserInteractionEvent) {
        when (event.commandName) {
            profileInfoCommandName -> handleProfileInfoCommand(event)
            else -> event.deleteReply().awaitFirstOrNull()
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

    fun createCommands(): List<ImmutableApplicationCommandRequest> {
        if (!plugin.configManager.linking.enabled) return listOf()

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

        return listOf(linkMinecraftCommand, userInfoCommand)
    }
}
