package com.dominikkorsa.discordintegration.update

data class PendingUpdate(
    val currentVersion: String,
    val latestVersion: String,
    val url: String,
)
