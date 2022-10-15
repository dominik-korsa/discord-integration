package com.dominikkorsa.discordintegration.utils

import net.md_5.bungee.api.chat.BaseComponent

fun BaseComponent.mapText(mapper: (text: String) -> String): BaseComponent {
    val result = duplicate()
    result.extra = result.extra.map { it.mapText(mapper) }
    return result
}

fun Iterable<BaseComponent>.mapText(mapper: (text: String) -> String) = map { it.mapText(mapper) }
