package com.dominikkorsa.discordintegration.plugin.utils

import java.util.*

inline fun <reified T> Any.tryCast() = if (this is T) this else null

fun <A, B> Pair<A, B>.swapped() = Pair(second, first)

fun <T> Optional<T>.orNull(): T? = if (isPresent) get() else null
