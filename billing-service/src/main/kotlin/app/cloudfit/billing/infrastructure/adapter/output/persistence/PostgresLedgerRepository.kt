package app.cloudfit.billing.infrastructure.adapter.output.persistence

import app.cloudfit.billing.application.port.output.LedgerRepository
import app.cloudfit.billing.domain.CreditBucket
import app.cloudfit.billing.domain.LedgerEntry
import app.cloudfit.billing.domain.LedgerReason
import app.cloudfit.shared.infrastructure.database.dbQuery
import app.cloudfit.shared.infrastructure.database.toUtc
import java.time.Instant
import java.util.UUID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll

class PostgresLedgerRepository : LedgerRepository {
    override suspend fun insertIfAbsent(entry: LedgerEntry): Boolean = dbQuery {
        CreditLedgerTable.insertIgnore {
            it[id] = entry.id
            it[userId] = entry.userId
            it[delta] = entry.delta
            it[bucket] = entry.bucket.name
            it[reason] = entry.reason.name
            it[refId] = entry.refId
            it[idempotencyKey] = entry.idempotencyKey
            it[createdAt] = entry.createdAt.toUtc()
        }.insertedCount > 0
    }

    override suspend fun findByRef(refId: String, reason: LedgerReason): List<LedgerEntry> = dbQuery {
        CreditLedgerTable.selectAll()
            .where { (CreditLedgerTable.refId eq refId) and (CreditLedgerTable.reason eq reason.name) }
            .map(::toEntry)
    }

    override suspend fun listByUser(userId: UUID, limit: Int): List<LedgerEntry> = dbQuery {
        CreditLedgerTable.selectAll()
            .where { CreditLedgerTable.userId eq userId }
            .orderBy(CreditLedgerTable.createdAt to SortOrder.DESC, CreditLedgerTable.id to SortOrder.DESC)
            .limit(limit)
            .map(::toEntry)
    }

    override suspend fun countByReasonSince(userId: UUID, reason: LedgerReason, since: Instant): Int = dbQuery {
        CreditLedgerTable.selectAll()
            .where {
                (CreditLedgerTable.userId eq userId) and
                    (CreditLedgerTable.reason eq reason.name) and
                    (CreditLedgerTable.createdAt greaterEq since.toUtc())
            }
            .count()
            .toInt()
    }

    private fun toEntry(row: ResultRow) = LedgerEntry(
        id = row[CreditLedgerTable.id],
        userId = row[CreditLedgerTable.userId],
        delta = row[CreditLedgerTable.delta],
        bucket = CreditBucket.valueOf(row[CreditLedgerTable.bucket]),
        reason = LedgerReason.valueOf(row[CreditLedgerTable.reason]),
        refId = row[CreditLedgerTable.refId],
        idempotencyKey = row[CreditLedgerTable.idempotencyKey],
        createdAt = row[CreditLedgerTable.createdAt].toInstant(),
    )
}
