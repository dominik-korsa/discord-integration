package com.dominikkorsa.discordintegration.dynmap

import org.bukkit.Bukkit
import org.dynmap.DynmapAPI

class DynmapIntegration private constructor(private val dynmap: DynmapAPI) {
    companion object {
        fun create(): DynmapIntegration? {
            return DynmapIntegration(
                Bukkit.getPluginManager().getPlugin("Dynmap") as DynmapAPI? ?: return null
            )
        }
    }

    fun sendMessage(content: String) {
        dynmap.sendBroadcastToWeb(null, content)
    }
}
