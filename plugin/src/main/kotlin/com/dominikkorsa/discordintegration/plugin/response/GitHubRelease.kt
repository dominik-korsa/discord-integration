package com.dominikkorsa.discordintegration.plugin.response

import kotlinx.serialization.Serializable

@Serializable
data class GitHubRelease(
    val tag_name: String,
    val body: String,
    val html_url: String,
)
