package com.dominikkorsa.discordintegration.luckperms

import com.dominikkorsa.discordintegration.DiscordIntegration
import net.luckperms.api.LuckPerms
import org.bukkit.Bukkit
import org.bukkit.plugin.RegisteredServiceProvider

fun registerLuckPerms(plugin: DiscordIntegration): Boolean {
    val provider: RegisteredServiceProvider<LuckPerms> =
        Bukkit.getServicesManager().getRegistration(LuckPerms::class.java) ?: return false
    val api: LuckPerms = provider.provider
    api.contextManager.registerCalculator(LinkedContextCalculator(plugin))
    return true
}
