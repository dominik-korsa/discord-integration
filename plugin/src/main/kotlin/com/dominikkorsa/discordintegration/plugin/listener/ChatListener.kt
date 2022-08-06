package com.dominikkorsa.discordintegration.plugin.listener

import com.dominikkorsa.discordintegration.plugin.DiscordIntegration
import com.dominikkorsa.discordintegration.plugin.config.ConfigManager.Debug.CancelledChatEventsMode.ALL
import com.dominikkorsa.discordintegration.plugin.config.ConfigManager.Debug.CancelledChatEventsMode.AUTO
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent

class ChatListener(private val plugin: DiscordIntegration) : Listener {
    private var anyNonCancelledReceived = false

    @EventHandler(priority = EventPriority.MONITOR)
    suspend fun onPlayerChat(event: AsyncPlayerChatEvent) {
        if (event.isCancelled) {
            val logCancelledEvents = plugin.configManager.debug.logCancelledChatEvents
            if (plugin.configManager.chat.ignoreCancelledChatEvents) {
                if (logCancelledEvents == ALL || (logCancelledEvents == AUTO && !anyNonCancelledReceived))
                    plugin.logger.warning(
                        "Message sent by player ${event.player.name} is ignored, because the AsyncPlayerChatEvent has been cancelled"
                    )
                plugin.logger.warning("You can try setting the config field `chat.ignore-cancelled-chat-events` to `false`")
                plugin.logger.warning("This might be caused by a chat plugin. Please report this issue on my Discord server")
                plugin.logger.warning("You can also disable this warning by setting `debug.log-cancelled-chat-events` to `disable`")
                return
            }
            if (logCancelledEvents == ALL) {
                plugin.logger.info("AsyncPlayerChatEvent of message sent by player ${event.player.name} has been cancelled")
                plugin.logger.info("Showing message anyways, because `chat.ignore-cancelled-chat-events` is set to `false`")
            }
        } else anyNonCancelledReceived = true

        plugin.sendChatToDiscord(event.player, event.message)
    }
}
