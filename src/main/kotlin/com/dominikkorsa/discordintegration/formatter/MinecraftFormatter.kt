package com.dominikkorsa.discordintegration.formatter

import com.dominikkorsa.discordintegration.DiscordIntegration
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull

class MinecraftFormatter(val plugin: DiscordIntegration) {
    suspend fun formatDiscordMessage(
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
            .replace("%content%", message.content.trimEnd())
    }

    fun formatHelpHeader(): String {
        return plugin.messageManager.commandsHelpHeader
            .replace("%plugin-version%", plugin.description.version)
    }

    fun formatHelpCommand(command: String, code: String): String {
        return plugin.messageManager.commandsHelpCommand
            .replace("%command%", command)
            .replace("%description%", plugin.messageManager.getCommandDescription(code))
    }
}
