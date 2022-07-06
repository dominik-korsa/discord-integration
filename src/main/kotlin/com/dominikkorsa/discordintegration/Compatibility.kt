package com.dominikkorsa.discordintegration

import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.BaseComponent
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object Compatibility {
    private val minecraftVersion = Regex("^\\d\\.(\\d+)").find(Bukkit.getBukkitVersion())
        ?.groups?.get(1)?.value?.toInt()
        ?: throw Exception("Cannot parse Bukkit version")

    fun Player.Spigot.sendChatMessage(vararg components: BaseComponent) {
        if (minecraftVersion >= 10) sendMessage(ChatMessageType.CHAT, *components)
        else sendMessage(*components)
    }

    fun Player.Spigot.sendSystemMessage(vararg components: BaseComponent) {
        if (minecraftVersion >= 10) sendMessage(ChatMessageType.SYSTEM, *components)
        else sendMessage(*components)
    }
}
