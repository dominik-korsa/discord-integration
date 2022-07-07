package com.dominikkorsa.discordintegration.config

import com.dominikkorsa.discordintegration.DiscordIntegration
import dev.dejvokep.boostedyaml.block.implementation.Section

class ConfigManager(plugin: DiscordIntegration): CustomConfig(plugin, "config.yml") {

    class Chat(private val section: Section) {
        class Embed(private val section: Section) {
            val enabled get() = section.getBooleanSafe("enabled")
            val color get() = section.getColor("color")
        }

        val channels get() = section.getStringListSafe("channels")
        val webhooks get() = section.getStringListSafe("webhooks")
        val playerAsStatusAuthor get() = section.getBooleanSafe("player-as-status-author")
        val avatarOfflineMode get() = section.getBooleanSafe("avatar.offline-mode")
        val avatarUrl get() = section.getTrimmedString("avatar.url")
        val joinEmbed get() = Embed(section.getSection("join-embed"))
        val quitEmbed get() = Embed(section.getSection("quit-embed"))
        val deathEmbed get() = Embed(section.getSection("death-embed"))
        val crashEmbed get() = Embed(section.getSection("crash-embed"))
    }

    class Activity(private val section: Section) {
        val updateInterval get() = section.getIntSafe("update-interval")
        val timeWorld get() = section.getTrimmedString("time.world")
        val timeRound get() = section.getIntSafe("time.round")
        val time24h get() = section.getBooleanSafe("time.24h")
        val idle get() = section.getBooleanSafe("idle-when-no-players-online")
    }

    class Linking(private val section: Section) {
        val enabled get() = section.getBooleanSafe("enabled")
        val mandatory get() = section.getBooleanSafe("mandatory")
        val linkedRoles get() = section.getStringListSafe("linked-roles")
        val notLinkedRoles get() = section.getStringListSafe("not-linked-roles")
        val syncNicknames get() = section.getBooleanSafe("sync-nicknames")
    }

    val discordToken get() = config.getString("discord-token")?.trim()

    val chat get() = Chat(config.getSection("chat"))
    val activity get() = Activity(config.getSection("activity"))
    val linking get() = Linking(config.getSection("linking"))
}
