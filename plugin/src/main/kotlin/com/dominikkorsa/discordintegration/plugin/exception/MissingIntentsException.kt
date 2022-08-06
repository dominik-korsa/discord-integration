package com.dominikkorsa.discordintegration.plugin.exception

class MissingIntentsException(val applicationId: Long) : Exception("Missing intents")
