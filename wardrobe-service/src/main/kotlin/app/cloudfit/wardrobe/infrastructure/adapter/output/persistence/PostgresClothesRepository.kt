package app.cloudfit.wardrobe.infrastructure.adapter.output.persistence

import app.cloudfit.shared.application.port.ClockProvider
import app.cloudfit.shared.domain.ClothingCategory
import app.cloudfit.shared.domain.Formality
import app.cloudfit.shared.domain.Warmth
import app.cloudfit.shared.infrastructure.database.dbQuery
import app.cloudfit.shared.infrastructure.database.toUtc
import app.cloudfit.wardrobe.application.port.output.ClothesRepository
import app.cloudfit.wardrobe.domain.Clothe
import java.util.UUID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.update

class PostgresClothesRepository(private val clock: ClockProvider) : ClothesRepository {
    override suspend fun listByUser(userId: UUID): List<Clothe> = dbQuery {
        ClothesTable.selectAll()
            .where { ClothesTable.userId eq userId }
            .orderBy(ClothesTable.createdAt to SortOrder.ASC)
            .map(::toClothe)
    }

    override suspend fun find(userId: UUID, id: UUID): Clothe? = dbQuery {
        ClothesTable.selectAll()
            .where { (ClothesTable.userId eq userId) and (ClothesTable.id eq id) }
            .map(::toClothe)
            .singleOrNull()
    }

    override suspend fun countByUser(userId: UUID): Long = dbQuery {
        ClothesTable.selectAll().where { ClothesTable.userId eq userId }.count()
    }

    override suspend fun insert(clothe: Clothe) {
        dbQuery {
            val now = clock.now().toUtc()
            ClothesTable.insert {
                it[id] = clothe.id
                it[userId] = clothe.userId
                it.fill(clothe)
                it[createdAt] = now
                it[updatedAt] = now
            }
        }
    }

    override suspend fun update(clothe: Clothe) {
        dbQuery {
            ClothesTable.update({ (ClothesTable.userId eq clothe.userId) and (ClothesTable.id eq clothe.id) }) {
                it.fill(clothe)
                it[updatedAt] = clock.now().toUtc()
            }
        }
    }

    override suspend fun delete(userId: UUID, id: UUID): Boolean = dbQuery {
        ClothesTable.deleteWhere { (ClothesTable.userId eq userId) and (ClothesTable.id eq id) } > 0
    }

    private fun UpdateBuilder<*>.fill(clothe: Clothe) {
        this[ClothesTable.category] = clothe.category.name
        this[ClothesTable.imageUrl] = clothe.imageUrl
        this[ClothesTable.name] = clothe.name
        this[ClothesTable.colour] = clothe.colour
        this[ClothesTable.pattern] = clothe.pattern
        this[ClothesTable.formality] = clothe.formality?.name
        this[ClothesTable.warmth] = clothe.warmth?.name
        this[ClothesTable.description] = clothe.description
    }

    private fun toClothe(row: ResultRow) = Clothe(
        id = row[ClothesTable.id],
        userId = row[ClothesTable.userId],
        category = ClothingCategory.valueOf(row[ClothesTable.category]),
        imageUrl = row[ClothesTable.imageUrl],
        name = row[ClothesTable.name],
        colour = row[ClothesTable.colour],
        pattern = row[ClothesTable.pattern],
        formality = row[ClothesTable.formality]?.let { Formality.valueOf(it) },
        warmth = row[ClothesTable.warmth]?.let { Warmth.valueOf(it) },
        description = row[ClothesTable.description],
    )
}
