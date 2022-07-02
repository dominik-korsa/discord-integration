package com.dominikkorsa.discordintegration.utils

inline fun<reified T> Any.tryCast() = if (this is T) this else null

fun<A, B> Pair<A, B>.swapped() = Pair(second, first)
