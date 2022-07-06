package com.dominikkorsa.discordintegration

import com.dominikkorsa.discordintegration.entities.PlayerEntity
import com.dominikkorsa.discordintegration.entities.Players
import org.bukkit.OfflinePlayer
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

class Db(plugin: DiscordIntegration) {
    init {
        val file = File(plugin.dataFolder, "database.db").absoluteFile
        Database.connect("jdbc:sqlite:$file", "org.sqlite.JDBC")

        transaction {
            SchemaUtils.create(Players)
        }
    }

    suspend fun getPlayer(player: OfflinePlayer): PlayerEntity {
        return newSuspendedTransaction {
            val dbPlayer = PlayerEntity.find { Players.id eq player.uniqueId }.singleOrNull()
            dbPlayer ?: PlayerEntity.new(player.uniqueId) {

            }
        }
    }
}
