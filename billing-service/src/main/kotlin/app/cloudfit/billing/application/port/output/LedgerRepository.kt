package app.cloudfit.billing.application.port.output

import app.cloudfit.billing.domain.LedgerEntry
import app.cloudfit.billing.domain.LedgerReason
import java.time.Instant
import java.util.UUID

interface LedgerRepository {
    suspend fun insertIfAbsent(entry: LedgerEntry): Boolean

    suspend fun findByRef(refId: String, reason: LedgerReason): List<LedgerEntry>

    suspend fun listByUser(userId: UUID, limit: Int): List<LedgerEntry>

    suspend fun countByReasonSince(userId: UUID, reason: LedgerReason, since: Instant): Int
}
