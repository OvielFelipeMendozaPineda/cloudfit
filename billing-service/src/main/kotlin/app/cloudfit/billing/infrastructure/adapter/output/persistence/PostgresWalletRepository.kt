package app.cloudfit.billing.infrastructure.adapter.output.persistence

import app.cloudfit.billing.application.port.output.WalletRepository
import app.cloudfit.billing.domain.CreditBucket
import app.cloudfit.billing.domain.WalletBucket
import app.cloudfit.shared.application.port.ClockProvider
import app.cloudfit.shared.infrastructure.database.dbQuery
import app.cloudfit.shared.infrastructure.database.toUtc
import java.time.Instant
import java.util.UUID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class PostgresWalletRepository(private val clock: ClockProvider) : WalletRepository {
    override suspend fun lockBuckets(userId: UUID): List<WalletBucket> = dbQuery {
        WalletBucketsTable.batchInsert(CreditBucket.entries, ignore = true, shouldReturnGeneratedValues = false) { bucket ->
            this[WalletBucketsTable.userId] = userId
            this[WalletBucketsTable.bucket] = bucket.name
            this[WalletBucketsTable.balance] = 0
            this[WalletBucketsTable.updatedAt] = clock.now().toUtc()
        }
        WalletBucketsTable.selectAll()
            .where { WalletBucketsTable.userId eq userId }
            .orderBy(WalletBucketsTable.bucket to SortOrder.ASC)
            .forUpdate()
            .map(::toBucket)
    }

    override suspend fun findBuckets(userId: UUID): List<WalletBucket> = dbQuery {
        WalletBucketsTable.selectAll()
            .where { WalletBucketsTable.userId eq userId }
            .map(::toBucket)
    }

    override suspend fun setBucket(userId: UUID, bucket: CreditBucket, balance: Int, expiresAt: Instant?) {
        dbQuery {
            WalletBucketsTable.update({ (WalletBucketsTable.userId eq userId) and (WalletBucketsTable.bucket eq bucket.name) }) {
                it[WalletBucketsTable.balance] = balance
                it[WalletBucketsTable.expiresAt] = expiresAt?.toUtc()
                it[updatedAt] = clock.now().toUtc()
            }
        }
    }

    private fun toBucket(row: ResultRow) = WalletBucket(
        bucket = CreditBucket.valueOf(row[WalletBucketsTable.bucket]),
        balance = row[WalletBucketsTable.balance],
        expiresAt = row[WalletBucketsTable.expiresAt]?.toInstant(),
    )
}
