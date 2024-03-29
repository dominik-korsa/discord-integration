package com.dominikkorsa.discordintegration.luckperms

import com.dominikkorsa.discordintegration.DiscordIntegration
import net.luckperms.api.LuckPerms
import org.bukkit.Bukkit

fun registerLuckPerms(plugin: DiscordIntegration): Boolean {
    val luckPermsClass = try {
        LuckPerms::class.java
    } catch (error: NoClassDefFoundError) {
        return false
    }
    val registration = Bukkit.getServicesManager().getRegistration(luckPermsClass) ?: return false
    registration.provider.contextManager.registerCalculator(LinkedContextCalculator(plugin))
    return true
}
