package com.dominikkorsa.discordintegration.utils

fun Long.floorBy(other: Long) = floorDiv(other) * other

fun <T : Comparable<T>> T.tryCompare(other: T) = if (this == other) null else this > other
