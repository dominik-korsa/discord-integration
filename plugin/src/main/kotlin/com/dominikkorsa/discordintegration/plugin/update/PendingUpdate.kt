package com.dominikkorsa.discordintegration.plugin.update

data class PendingUpdate(
    val currentVersion: String,
    val latestVersion: String,
    val url: String,
)
