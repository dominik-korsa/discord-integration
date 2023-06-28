package com.dominikkorsa.discordintegration.utils

import discord4j.core.spec.EmbedCreateSpec
import discord4j.core.spec.WebhookExecuteSpec
import discord4j.discordjson.json.ApplicationCommandOptionData
import discord4j.discordjson.json.ApplicationCommandRequest
import discord4j.discordjson.json.ImmutableApplicationCommandOptionData
import discord4j.discordjson.json.ImmutableApplicationCommandRequest

fun WebhookExecuteSpec.Builder.addEmbed(build: EmbedCreateSpec.Builder.() -> Unit) {
    addEmbed(
        EmbedCreateSpec.builder().apply(build).build()
    )
}

fun createApplicationCommand(
    build: ImmutableApplicationCommandRequest.Builder.() -> Unit,
): ImmutableApplicationCommandRequest = ApplicationCommandRequest.builder().apply(build).build()

fun ImmutableApplicationCommandRequest.Builder.addOption(
    build: ImmutableApplicationCommandOptionData.Builder.() -> Unit,
) {
    addOption(ApplicationCommandOptionData.builder().apply(build).build())
}
