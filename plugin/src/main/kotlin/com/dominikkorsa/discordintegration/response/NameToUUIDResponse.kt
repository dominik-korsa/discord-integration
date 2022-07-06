package com.dominikkorsa.discordintegration.response

import kotlinx.serialization.Serializable

@Serializable
data class NameToUUIDResponse(
    val id: String,
    val name: String,
)
