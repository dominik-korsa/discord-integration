package com.dominikkorsa.discordintegration.exception

class ConfigNotSetException(path: String) : Exception("Field $path not set in config.yml")
class MessageNotSetException(path: String) : Exception("Field $path not set in messages.yml")
