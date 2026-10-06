package app.cloudfit.styling.infrastructure.adapter.output.persistence

import app.cloudfit.shared.application.port.ClockProvider
import app.cloudfit.shared.infrastructure.database.dbQuery
import app.cloudfit.shared.infrastructure.database.toUtc
import app.cloudfit.styling.application.port.output.LookRepository
import app.cloudfit.styling.domain.Look
import app.cloudfit.styling.domain.LookStatus
import java.time.Instant
import java.util.UUID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class PostgresLookRepository(private val clock: ClockProvider) : LookRepository {
    private val inProgress = LookStatus.IN_PROGRESS.map { it.name }

    override suspend fun create(look: Look) {
        dbQuery {
            val now = clock.now().toUtc()
            LooksTable.insert {
                it[id] = look.id
                it[userId] = look.userId
                it[event] = look.event
                it[status] = look.status.name
                it[clotheIds] = look.clotheIds
                it[stylistNote] = look.stylistNote
                it[imageUrl] = look.imageUrl
                it[saved] = look.saved
                it[failureCode] = look.failureCode
                it[createdAt] = look.createdAt.toUtc()
                it[updatedAt] = now
            }
        }
    }

    override suspend fun findById(id: UUID): Look? = dbQuery {
        LooksTable.selectAll().where { LooksTable.id eq id }.map(::toLook).singleOrNull()
    }

    override suspend fun find(userId: UUID, id: UUID): Look? = dbQuery {
        LooksTable.selectAll()
            .where { (LooksTable.userId eq userId) and (LooksTable.id eq id) }
            .map(::toLook)
            .singleOrNull()
    }

    override suspend fun list(userId: UUID, savedOnly: Boolean, limit: Int): List<Look> = dbQuery {
        LooksTable.selectAll()
            .where { if (savedOnly) (LooksTable.userId eq userId) and (LooksTable.saved eq true) else LooksTable.userId eq userId }
            .orderBy(LooksTable.createdAt to SortOrder.DESC)
            .limit(limit)
            .map(::toLook)
    }

    override suspend fun updateProgress(look: Look): Boolean = dbQuery {
        LooksTable.update({ (LooksTable.id eq look.id) and (LooksTable.status inList inProgress) }) {
            it[status] = look.status.name
            it[clotheIds] = look.clotheIds
            it[stylistNote] = look.stylistNote
            it[imageUrl] = look.imageUrl
            it[failureCode] = look.failureCode
            it[updatedAt] = clock.now().toUtc()
        } > 0
    }

    override suspend fun failIfStale(id: UUID, failureCode: String, updatedBefore: Instant): Boolean = dbQuery {
        LooksTable.update({
            (LooksTable.id eq id) and (LooksTable.status inList inProgress) and (LooksTable.updatedAt less updatedBefore.toUtc())
        }) {
            it[status] = LookStatus.FAILED.name
            it[LooksTable.failureCode] = failureCode
            it[updatedAt] = clock.now().toUtc()
        } > 0
    }

    override suspend fun findStale(updatedBefore: Instant): List<Look> = dbQuery {
        LooksTable.selectAll()
            .where { (LooksTable.status inList inProgress) and (LooksTable.updatedAt less updatedBefore.toUtc()) }
            .limit(500)
            .map(::toLook)
    }

    override suspend fun markSaved(userId: UUID, id: UUID) {
        dbQuery {
            LooksTable.update({ (LooksTable.userId eq userId) and (LooksTable.id eq id) }) {
                it[saved] = true
                it[updatedAt] = clock.now().toUtc()
            }
        }
    }

    override suspend fun delete(userId: UUID, id: UUID): Boolean = dbQuery {
        LooksTable.deleteWhere { (LooksTable.userId eq userId) and (LooksTable.id eq id) } > 0
    }

    private fun toLook(row: ResultRow) = Look(
        id = row[LooksTable.id],
        userId = row[LooksTable.userId],
        event = row[LooksTable.event],
        status = LookStatus.valueOf(row[LooksTable.status]),
        clotheIds = row[LooksTable.clotheIds],
        stylistNote = row[LooksTable.stylistNote],
        imageUrl = row[LooksTable.imageUrl],
        saved = row[LooksTable.saved],
        failureCode = row[LooksTable.failureCode],
        createdAt = row[LooksTable.createdAt].toInstant(),
    )
}
