package com.dominikkorsa.discordintegration.exception

class ConfigNotSetException(path: String, filename: String) : Exception("Field $path not set in $filename")
