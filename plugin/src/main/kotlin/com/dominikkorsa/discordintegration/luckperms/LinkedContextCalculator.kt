package com.dominikkorsa.discordintegration.luckperms

import com.dominikkorsa.discordintegration.DiscordIntegration
import net.luckperms.api.context.ContextCalculator
import net.luckperms.api.context.ContextConsumer
import net.luckperms.api.context.ContextSet
import net.luckperms.api.context.ImmutableContextSet
import org.bukkit.OfflinePlayer

class LinkedContextCalculator(private val plugin: DiscordIntegration) : ContextCalculator<OfflinePlayer> {
    companion object {
        const val contextName = "di:is-linked"
    }

    override fun calculate(player: OfflinePlayer, consumer: ContextConsumer) {
        consumer.accept(contextName, (plugin.db.getDiscordId(player.uniqueId) != null).toString())
    }

    override fun estimatePotentialContexts(): ContextSet {
        val builder = ImmutableContextSet.builder()
        builder.add(contextName, "false")
        builder.add(contextName, "true")
        return builder.build()
    }
}
