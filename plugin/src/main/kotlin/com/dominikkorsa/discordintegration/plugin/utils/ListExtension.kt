package com.dominikkorsa.discordintegration.plugin.utils

fun <T> List<T>.joinToList(separator: (prevElement: T) -> Array<out T>): List<T> {
    if (isEmpty()) return emptyList()
    return dropLast(1).flatMap { listOf(it, *separator(it)) }.plus(last())
}

fun <T> List<T>.joinToList(vararg separators: T): List<T> = joinToList { separators }

fun <T, R> List<T>.mapAndJoin(mapper: (element: T) -> R, separator: (prevElement: T) -> R): List<R> {
    if (isEmpty()) return emptyList()
    return dropLast(1).flatMap { listOf(mapper(it), separator(it)) }.plus(mapper(last()))
}
