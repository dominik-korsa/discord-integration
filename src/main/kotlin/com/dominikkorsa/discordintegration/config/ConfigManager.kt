package com.dominikkorsa.discordintegration.config

import com.dominikkorsa.discordintegration.DiscordIntegration

class ConfigManager(plugin: DiscordIntegration): CustomConfig(plugin, "config.yml") {

    interface Linking {
        val enabled: Boolean
        val mandatory: Boolean
        val linkedRoles: MutableList<String>
        val notLinkedRoles: MutableList<String>
        val syncNicknames: Boolean
    }

    private fun getEmbedConfig(path: String): EmbedConfig {
        return EmbedConfig(
            config.getBoolean("$path.enabled"),
            getColor("$path.color")
        )
    }

    val discordToken get() = getString("discord-token")
    val chatChannels: List<String> get() = config.getStringList("chat.channels")
    val chatWebhooks: List<String> get() = config.getStringList("chat.webhooks")
    val playerAsStatusAuthor get() = getBoolean("chat.player-as-status-author")
    val avatarOfflineMode get() = getBoolean("chat.avatar.offline-mode")
    val avatarUrl get() = getString("chat.avatar.url")

    val activityUpdateInterval get() = getInt("activity.update-interval")
    val activityTimeWorld get() = getString("activity.time.world")
    val activityTimeRound get() = getInt("activity.time.round")
    val activityTime24h get() = getBoolean("activity.time.24h")
    val activityIdle get() = getBoolean("activity.idle-when-no-players-online")

    val joinEmbed get() = getEmbedConfig("chat.join-embed")
    val quitEmbed get() = getEmbedConfig("chat.quit-embed")
    val deathEmbed get() = getEmbedConfig("chat.death-embed")
    val crashEmbed get() = getEmbedConfig("chat.crash-embed")

    val linking = object: Linking {
        override val enabled get() = getBoolean("linking.enabled")
        override val mandatory get() = getBoolean("linking.mandatory")
        override val linkedRoles get() = config.getStringList("linking.linked-roles")
        override val notLinkedRoles get() = config.getStringList("linking.not-linked-roles")
        override val syncNicknames get() = config.getBoolean("linking.sync-nicknames")
    }
}
