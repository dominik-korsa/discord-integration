package com.dominikkorsa.discordintegration.plugin.exception

class ConfigNotSetException(route: String) : Exception("Field $route not set") {
    constructor(parent: String?, route: String) : this(parent?.let { "$it.$route" } ?: route)
}
