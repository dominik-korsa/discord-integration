package com.dominikkorsa.discordintegration.exception

class MissingIntentsException(val applicationId: Long) : Exception("Missing intents")
