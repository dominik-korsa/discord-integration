package com.dominikkorsa.discordintegration.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CatchUnknown
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import com.dominikkorsa.discordintegration.DiscordIntegration
import com.github.shynixn.mccoroutine.launchAsync
import org.bukkit.command.CommandSender

@CommandAlias("discordintegration|di")
class DiscordIntegrationCommand(val plugin: DiscordIntegration): BaseCommand() {
    @Subcommand("reload")
    fun onReload() {
        plugin.launchAsync {
            plugin.disconnect()
            plugin.configManager.reload()
            plugin.messageManager.reload()
            plugin.connect()
        }
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
        sender.sendMessage(plugin.messageManager.commandsUnknown)
    }

    private fun sendHelpCommandItem(
        sender: CommandSender,
        command: String,
        code: String
    ) {
        sender.sendMessage(plugin.minecraftFormatter.formatHelpCommand(command, code))
    }
}
