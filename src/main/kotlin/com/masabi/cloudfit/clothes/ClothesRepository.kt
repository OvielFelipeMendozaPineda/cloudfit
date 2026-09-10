package com.masabi.cloudfit.clothes

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

class ClothesRepository {

    fun all(): List<Clothe> = transaction {
        ClothesTable.selectAll().map { it.toClothe() }
    }

    fun get(id: String): Clothe? = transaction {
        ClothesTable.selectAll().where { ClothesTable.id eq id }.map { it.toClothe() }.singleOrNull()
    }

    fun save(clothe: Clothe): Clothe = transaction {
        val stored = clothe.copy(id = clothe.id.ifBlank { UUID.randomUUID().toString() })
        val exists = ClothesTable.selectAll().where { ClothesTable.id eq stored.id }.any()
        if (exists) {
            ClothesTable.update({ ClothesTable.id eq stored.id }) { it.fromClothe(stored) }
        } else {
            ClothesTable.insert { it.fromClothe(stored) }
        }
        stored
    }

    fun delete(id: String) {
        transaction { ClothesTable.deleteWhere { ClothesTable.id eq id } }
    }
}

private fun UpdateBuilder<*>.fromClothe(clothe: Clothe) {
    this[ClothesTable.id] = clothe.id
    this[ClothesTable.category] = clothe.category.name
    this[ClothesTable.imageUrl] = clothe.imageUrl
    this[ClothesTable.sourceImageUrl] = clothe.sourceImageUrl
    this[ClothesTable.name] = clothe.name
    this[ClothesTable.colour] = clothe.colour
    this[ClothesTable.pattern] = clothe.pattern
    this[ClothesTable.formality] = clothe.formality?.name
    this[ClothesTable.warmth] = clothe.warmth?.name
    this[ClothesTable.description] = clothe.description
    this[ClothesTable.sourceType] = clothe.source.name
}

private fun ResultRow.toClothe() = Clothe(
    id = this[ClothesTable.id],
    category = ClothingCategory.valueOf(this[ClothesTable.category]),
    imageUrl = this[ClothesTable.imageUrl],
    sourceImageUrl = this[ClothesTable.sourceImageUrl],
    name = this[ClothesTable.name],
    colour = this[ClothesTable.colour],
    pattern = this[ClothesTable.pattern],
    formality = this[ClothesTable.formality]?.let { Formality.valueOf(it) },
    warmth = this[ClothesTable.warmth]?.let { Warmth.valueOf(it) },
    description = this[ClothesTable.description],
    source = ClotheSource.valueOf(this[ClothesTable.sourceType]),
)
