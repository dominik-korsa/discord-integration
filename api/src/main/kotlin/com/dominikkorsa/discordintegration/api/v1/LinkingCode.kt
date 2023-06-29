package com.dominikkorsa.discordintegration.api.v1

import org.bukkit.entity.Player

@Suppress("unused")
interface LinkingCode {
    val code: String

    val player: Player

    val isValid: Boolean
}
