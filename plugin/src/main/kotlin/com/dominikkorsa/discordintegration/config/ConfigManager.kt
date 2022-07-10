package com.dominikkorsa.discordintegration.config

import com.dominikkorsa.discordintegration.DiscordIntegration
import dev.dejvokep.boostedyaml.block.implementation.Section

class ConfigManager(plugin: DiscordIntegration): CustomConfig(plugin, "config.yml") {

    class Chat(private val section: Section) {
        class Embed(private val section: Section) {
            val enabled get() = section.requireBoolean("enabled")
            val color get() = section.getColor("color")
        }

        val channels get() = section.requireStringList("channels")
        val webhooks get() = section.requireStringList("webhooks")
        val consoleChannels get() = section.requireStringList("console-channels")
        val playerAsStatusAuthor get() = section.requireBoolean("player-as-status-author")
        val avatarOfflineMode get() = section.requireBoolean("avatar.offline-mode")
        val avatarUrl get() = section.requireTrimmedString("avatar.url")
        val joinEmbed get() = Embed(section.getSection("join-embed"))
        val quitEmbed get() = Embed(section.getSection("quit-embed"))
        val deathEmbed get() = Embed(section.getSection("death-embed"))
        val crashEmbed get() = Embed(section.getSection("crash-embed"))
    }

    class Activity(private val section: Section) {
        val updateInterval get() = section.requireInt("update-interval")
        val timeWorld get() = section.requireTrimmedString("time.world")
        val timeRound get() = section.requireInt("time.round")
        val time24h get() = section.requireBoolean("time.24h")
        val idle get() = section.requireBoolean("idle-when-no-players-online")
    }

    class Linking(private val section: Section) {
        val enabled get() = section.requireBoolean("enabled")
        val mandatory get() = section.requireBoolean("mandatory")
        val linkedRoles get() = section.requireStringList("linked-roles")
        val notLinkedRoles get() = section.requireStringList("not-linked-roles")
        val syncNicknames get() = section.requireBoolean("sync-nicknames")
    }

    class Debug(private val section: Section) {
        val logDiscordMessages get() = section.requireBoolean("log-discord-messages")
    }

    val discordToken get() = config.getString("discord-token")?.trim()

    val chat get() = Chat(config.getSection("chat"))
    val activity get() = Activity(config.getSection("activity"))
    val linking get() = Linking(config.getSection("linking"))
    val debug get() = Debug(config.getSection("debug"))
}
