package com.dominikkorsa.discordintegration.exception

class ConfigNotSetException(field: String) : Exception("Field $field not set in config")
