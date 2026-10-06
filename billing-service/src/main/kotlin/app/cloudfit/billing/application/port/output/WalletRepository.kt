package app.cloudfit.billing.application.port.output

import app.cloudfit.billing.domain.CreditBucket
import app.cloudfit.billing.domain.WalletBucket
import java.time.Instant
import java.util.UUID

interface WalletRepository {
    suspend fun lockBuckets(userId: UUID): List<WalletBucket>

    suspend fun findBuckets(userId: UUID): List<WalletBucket>

    suspend fun setBucket(userId: UUID, bucket: CreditBucket, balance: Int, expiresAt: Instant?)
}
