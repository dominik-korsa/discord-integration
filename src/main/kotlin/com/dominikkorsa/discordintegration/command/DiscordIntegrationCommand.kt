package com.dominikkorsa.discordintegration.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import com.dominikkorsa.discordintegration.DiscordIntegration
import com.github.shynixn.mccoroutine.launchAsync
import org.bukkit.command.CommandSender

@CommandAlias("discordintegration|di")
class DiscordIntegrationCommand(val plugin: DiscordIntegration): BaseCommand() {
    @Subcommand("reload")
    @CommandPermission("discordintegration.command.reload")
    fun onReload() {
        plugin.launchAsync { plugin.reload() }
    }

    @Subcommand("help")
    @Default
    fun onHelp(sender: CommandSender) {
        sender.sendMessage(plugin.minecraftFormatter.formatHelpHeader())
        sendHelpCommandItem(sender, "/di help", "help")
        sendHelpCommandItem(sender, "/di reload", "reload")
    }

    @CatchUnknown
    fun onUnknown(sender: CommandSender) {
        sender.sendMessage(plugin.messages.commandsUnknown)
    }

    private fun sendHelpCommandItem(
        sender: CommandSender,
        command: String,
        code: String
    ) {
        sender.sendMessage(plugin.minecraftFormatter.formatHelpCommand(command, code))
    }
}
