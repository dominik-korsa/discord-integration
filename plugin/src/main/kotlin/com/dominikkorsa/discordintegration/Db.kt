package com.dominikkorsa.discordintegration

import com.charleskorn.kaml.Yaml
import discord4j.common.util.Snowflake
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.Bukkit
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.io.File
import java.util.*

class Db(private val plugin: DiscordIntegration) {
    object LegacyPlayersColumn : UUIDTable("Players") {
        val discordId = long("discordId").nullable().uniqueIndex()
    }

    class LegacyPlayerEntity(id: EntityID<UUID>) : UUIDEntity(id) {
        companion object : UUIDEntityClass<LegacyPlayerEntity>(LegacyPlayersColumn)

        var discordId by LegacyPlayersColumn.discordId
    }

    private object SnowflakeSerializer : KSerializer<Snowflake> {
        override val descriptor = PrimitiveSerialDescriptor("Snowflake", PrimitiveKind.LONG)
        override fun deserialize(decoder: Decoder): Snowflake = Snowflake.of(decoder.decodeLong())
        override fun serialize(encoder: Encoder, value: Snowflake) {
            encoder.encodeLong(value.asLong())
        }
    }

    private object UUIDSerializer : KSerializer<UUID> {
        override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)
        override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
        override fun serialize(encoder: Encoder, value: UUID) {
            encoder.encodeString(value.toString())
        }
    }

    @Serializable
    data class Player(
        @Serializable(with = SnowflakeSerializer::class)
        val discordId: Snowflake?,
    ) {
        fun withDiscordId(discordId: Snowflake?) = Player(discordId)
    }

    private val playerMapSerializer = MapSerializer(UUIDSerializer, Player.serializer())

    private var players = HashMap<UUID, Player>()
    private val file = File(plugin.dataFolder, "players.yml")

    private suspend fun migrateDb() {
        withContext(IO) {
            val legacyDbFile = File(plugin.dataFolder, "database.db").absoluteFile
            if (!legacyDbFile.exists()) return@withContext
            plugin.logger.warning("Migrating player database to YAML")
            try {
                Database.connect("jdbc:sqlite:$legacyDbFile", "org.sqlite.JDBC")
                newSuspendedTransaction {
                    players.putAll(LegacyPlayerEntity.all().map {
                        Pair(it.id.value, Player(it.discordId?.let(Snowflake::of)))
                    })
                }
                save()
                plugin.logger.warning("Migration complete")
                val newFile = File(legacyDbFile.parent, "database-backup.db")
                legacyDbFile.copyTo(newFile)
                plugin.logger.warning("Created database backup at $newFile")
                legacyDbFile.delete()
                legacyDbFile.deleteOnExit()
            } catch (error: Exception) {
                plugin.logger.severe("Failed to migrate player database")
                plugin.logger.severe(error.toString())
            }
        }
    }

    private suspend fun save() {
        withContext(IO) {
            Yaml.default.encodeToStream(playerMapSerializer, players, file.outputStream())
        }
    }

    private suspend fun load() {
        withContext(IO) {
            if (file.exists()) {
                players = HashMap(Yaml.default.decodeFromStream(playerMapSerializer, file.inputStream()))
            } else save()
        }
    }

    suspend fun reload() {
        load()
        buildIndexes()
    }

    suspend fun init() {
        load()
        migrateDb()
        buildIndexes()
    }

    private var playerOfMember = HashMap<Snowflake, UUID>()

    private fun nameFromUUID(uniqueId: UUID) = Bukkit.getOfflinePlayer(uniqueId).name

    private fun buildIndexes() {
        playerOfMember.clear()
        players.forEach {
            it.value.discordId?.let { discordId ->
                playerOfMember.putIfAbsent(discordId, it.key)?.let { existing ->
                    players[it.key] = it.value.withDiscordId(null)
                    plugin.logger.warning("Conflict while loading players database")
                    plugin.logger.warning(
                        "Discord account with ID $discordId is already linked to player ${
                            nameFromUUID(existing) ?: existing.toString()
                        }, unlinking ${nameFromUUID(it.key) ?: it.key.toString()}"
                    )
                }
            }
        }
    }

    private fun getOrCreatePlayer(playerId: UUID) = players.getOrPut(playerId) { Player(null) }

    fun getDiscordId(playerId: UUID) = players[playerId]?.discordId

    fun playerIdOfMember(discordId: Snowflake) = playerOfMember[discordId]

    suspend fun resetDiscordId(playerId: UUID): Snowflake? {
        val previousPlayer = getOrCreatePlayer(playerId)
        if (previousPlayer.discordId == null) return null
        playerOfMember.remove(previousPlayer.discordId)
        players[playerId] = previousPlayer.withDiscordId(null)
        save()
        return previousPlayer.discordId
    }

    suspend fun setDiscordId(playerId: UUID, discordId: Snowflake): Pair<UUID?, Snowflake?>? {
        val previouslyLinkedPlayerId = playerOfMember.put(discordId, playerId)
        if (previouslyLinkedPlayerId == playerId) return null

        val playerBefore = getOrCreatePlayer(playerId)
        playerBefore.discordId?.let(playerOfMember::remove)
        players[playerId] = playerBefore.withDiscordId(discordId)
        save()
        return Pair(previouslyLinkedPlayerId, playerBefore.discordId)
    }
}
