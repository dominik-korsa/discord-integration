package com.dominikkorsa.discordintegration.exception

class ConfigNotSetException(path: String) : Exception("Field $path")
