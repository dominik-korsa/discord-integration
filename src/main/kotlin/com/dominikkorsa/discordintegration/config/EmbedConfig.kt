package com.dominikkorsa.discordintegration.config

import discord4j.rest.util.Color

data class EmbedConfig(
    val enabled: Boolean,
    val color: Color,
)
