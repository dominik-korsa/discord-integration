package com.dominikkorsa.discordintegration.tps

import net.minecraft.server.v1_16_R3.MinecraftServer
import kotlin.math.min

class TpsService {
    private val server = MinecraftServer.getServer()

    fun getRecentTps(): Tps {
        val recentTps = server.recentTps.map { min(it, 20.0) }
        return Tps(
            of1min = recentTps.elementAt(0),
            of5min = recentTps.elementAt(1),
            of15min = recentTps.elementAt(2)
        )
    }
}
