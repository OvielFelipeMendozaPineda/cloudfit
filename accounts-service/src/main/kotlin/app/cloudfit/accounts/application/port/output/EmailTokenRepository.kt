package app.cloudfit.accounts.application.port.output

import app.cloudfit.accounts.domain.EmailToken
import app.cloudfit.accounts.domain.EmailTokenPurpose
import java.time.Instant
import java.util.UUID

interface EmailTokenRepository {
    suspend fun create(token: EmailToken)

    suspend fun lockValid(tokenHash: String, purpose: EmailTokenPurpose, now: Instant): EmailToken?

    suspend fun markUsed(id: UUID, at: Instant)

    suspend fun invalidateAll(userId: UUID, purpose: EmailTokenPurpose, at: Instant)
}
