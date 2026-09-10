package com.masabi.cloudfit.outfit

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class OutfitsRepository {

    fun all(): List<Outfit> = transaction {
        OutfitsTable.selectAll().map { it.toOutfit() }
    }

    fun get(id: String): Outfit? = transaction {
        OutfitsTable.selectAll().where { OutfitsTable.id eq id }.map { it.toOutfit() }.singleOrNull()
    }

    fun save(outfit: Outfit): Outfit = transaction {
        val exists = OutfitsTable.selectAll().where { OutfitsTable.id eq outfit.id }.any()
        if (exists) {
            OutfitsTable.update({ OutfitsTable.id eq outfit.id }) { it.fromOutfit(outfit) }
        } else {
            OutfitsTable.insert { it.fromOutfit(outfit) }
        }
        outfit
    }
}

private fun UpdateBuilder<*>.fromOutfit(outfit: Outfit) {
    this[OutfitsTable.id] = outfit.id
    this[OutfitsTable.event] = outfit.event ?: ""
    this[OutfitsTable.clotheIds] = outfit.clotheIds.joinToString(",")
    this[OutfitsTable.stylistNote] = outfit.stylistNote
    this[OutfitsTable.imageStatus] = outfit.imageStatus.name
    this[OutfitsTable.imageUrl] = outfit.imageUrl
}

private fun ResultRow.toOutfit() = Outfit(
    id = this[OutfitsTable.id],
    event = this[OutfitsTable.event],
    clotheIds = this[OutfitsTable.clotheIds].split(",").filter { it.isNotBlank() },
    stylistNote = this[OutfitsTable.stylistNote],
    imageStatus = ImageStatus.valueOf(this[OutfitsTable.imageStatus]),
    imageUrl = this[OutfitsTable.imageUrl],
)
