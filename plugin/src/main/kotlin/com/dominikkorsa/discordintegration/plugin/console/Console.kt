package com.dominikkorsa.discordintegration.plugin.console

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger

class Console {
    private val rootLogger get() = LogManager.getRootLogger() as Logger
    private var appender: ConsoleAppender? = null

    fun start() = callbackFlow {
        if (appender != null) throw Exception("Attempting to start already started appender")
        appender = ConsoleAppender { message -> this.trySend(message) }.apply {
            start()
            rootLogger.addAppender(this)
        }
        awaitClose { stop() }
    }

    fun stop() {
        appender?.let {
            rootLogger.removeAppender(it)
            it.stop()
        }
        appender = null
    }
}
