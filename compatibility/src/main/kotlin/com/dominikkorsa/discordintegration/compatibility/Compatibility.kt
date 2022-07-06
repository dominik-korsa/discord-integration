package com.dominikkorsa.discordintegration.compatibility

import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.HoverEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object Compatibility {
    private val minecraftVersion = Regex("^\\d\\.(\\d+)").find(Bukkit.getBukkitVersion())
        ?.groups?.get(1)?.value?.toInt()
        ?: throw Exception("Cannot parse Bukkit version")

    fun sendChatMessage(player: Player, vararg components: BaseComponent) {
        if (minecraftVersion >= 10) player.spigot().sendMessage(ChatMessageType.CHAT, *components)
        else player.spigot().sendMessage(*components)
    }

    fun sendSystemMessage(player: Player, vararg components: BaseComponent) {
        if (minecraftVersion >= 10) player.spigot().sendMessage(ChatMessageType.SYSTEM, *components)
        else player.spigot().sendMessage(*components)
    }

    fun BaseComponent.setCopyToClipboard(value: String, tooltip: Array<BaseComponent>? = null): Boolean {
        if (minecraftVersion < 16) return false
        clickEvent = ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, value)
        if (tooltip !== null) {
            @Suppress("DEPRECATION")
            hoverEvent = HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                tooltip
            )
        }
        return true
    }
}
