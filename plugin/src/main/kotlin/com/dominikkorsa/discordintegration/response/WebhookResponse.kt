package com.dominikkorsa.discordintegration.response

import kotlinx.serialization.Serializable

@Serializable
/**
 * https://discord.com/developers/docs/resources/webhook
 */
data class WebhookResponse(
    val id: String,
    val channel_id: String?,
)
