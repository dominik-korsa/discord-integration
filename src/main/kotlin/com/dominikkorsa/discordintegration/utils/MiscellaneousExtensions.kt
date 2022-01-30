package com.dominikkorsa.discordintegration.utils

inline fun<reified T> Any.tryCast() = if (this is T) this else null
