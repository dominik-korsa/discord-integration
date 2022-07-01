package com.dominikkorsa.discordintegration

import org.bukkit.OfflinePlayer
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.util.*

class Db(plugin: DiscordIntegration) {
    init {
        val file = File(plugin.dataFolder, "database.db").absoluteFile
        Database.connect("jdbc:sqlite:$file", "org.sqlite.JDBC")

        transaction {
            SchemaUtils.create(Players)
        }
    }

    suspend fun getPlayer(player: OfflinePlayer): Player {
        return newSuspendedTransaction {
            val dbPlayer = Player.find { Players.id eq player.uniqueId }.singleOrNull()
            dbPlayer ?: Player.new(player.uniqueId) {

            }
        }
    }
}

object Players : UUIDTable() {
    val discordId = integer("discordId").nullable().uniqueIndex()
}

class Player(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Player>(Players)
    var discordId by Players.discordId
}
