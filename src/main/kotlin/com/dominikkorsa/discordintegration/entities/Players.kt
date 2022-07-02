package com.dominikkorsa.discordintegration.entities

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

object Players : UUIDTable() {
    val discordId = long("discordId").nullable().uniqueIndex()
}

class PlayerEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<PlayerEntity>(Players)
    var discordId by Players.discordId
}
