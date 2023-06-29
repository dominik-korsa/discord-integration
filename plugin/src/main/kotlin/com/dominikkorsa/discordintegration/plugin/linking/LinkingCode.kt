package com.dominikkorsa.discordintegration.plugin.linking

import com.dominikkorsa.discordintegration.api.v1.LinkingCode
import kotlinx.coroutines.delay
import org.bukkit.entity.Player
import java.time.Duration
import java.time.LocalDateTime.now
import kotlin.time.toKotlinDuration

class LinkingCode(
    override val code: String,
    override val player: Player
): LinkingCode {
    private val validUntil = now() + Duration.ofMinutes(10)

    private var used = false

    override val isValid: Boolean
        get() = !used && validUntil.isAfter(now())

    internal suspend fun waitUntilInvalid() {
        if (!isValid) return
        delay(Duration.between(now(), validUntil).toKotlinDuration())
    }

    internal fun use() {
        used = true
    }
}
