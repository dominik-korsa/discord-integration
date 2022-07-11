package com.dominikkorsa.discordintegration.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlin.time.Duration

@OptIn(FlowPreview::class)
fun Flow<String>.bunchLines(
    timeout: Duration,
    maxLength: Int,
): Flow<String> = channelFlow {
    var queued = ""
    var job: Job? = null
    suspend fun flush() {
        job?.cancelAndJoin()
    }

    fun start() {
        if (job != null) return
        job = launch {
            try {
                delay(timeout)
            } catch (_: CancellationException) {
            }
            send(queued)
            queued = ""
            job = null
        }
    }
    collect {
        it.lines().flatMap { line -> line.chunked(maxLength - 1) }.forEach { line ->
            if (queued.length + line.length > maxLength - 1) flush()
            queued += "$line\n"
            start()
        }
    }
}
