package com.dominikkorsa.discordintegration.linking

import kotlinx.coroutines.delay
import org.bukkit.OfflinePlayer
import java.time.Duration
import java.time.LocalDateTime.now
import kotlin.time.toKotlinDuration

class LinkingCode(
    val code: String,
    private val player: OfflinePlayer
) {
    private val validUntil = now() + Duration.ofMinutes(10)

    private var used = false

    fun isValid(): Boolean {
        return !used && validUntil.isAfter(now())
    }

    suspend fun waitUntilInvalid() {
        if (!isValid()) return
        delay(Duration.between(now(), validUntil).toKotlinDuration())
    }

    fun use() {
        used = true
    }
}
