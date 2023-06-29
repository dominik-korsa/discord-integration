package com.dominikkorsa.discordintegration.api.v1

import discord4j.core.`object`.entity.Message
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

@Suppress("unused")
class DiscordIntegrationMessageEvent(val message: Message) : Event(), Cancellable {
    companion object {
        private val handlers = HandlerList()

        @JvmStatic
        @Suppress("unused")
        fun getHandlerList() = handlers
    }

    private var isCancelled = false

    override fun setCancelled(cancel: Boolean) {
        isCancelled = cancel
    }

    override fun isCancelled() = isCancelled

    override fun getHandlers() = Companion.handlers
}
