package app.cloudfit.accounts.domain

import java.time.Instant
import java.util.UUID

enum class EmailTokenPurpose { VERIFY, RESET }

data class EmailToken(
    val id: UUID,
    val userId: UUID,
    val tokenHash: String,
    val purpose: EmailTokenPurpose,
    val passwordHash: String?,
    val expiresAt: Instant,
    val usedAt: Instant?,
)

data class RefreshToken(
    val id: UUID,
    val userId: UUID,
    val familyId: UUID,
    val tokenHash: String,
    val expiresAt: Instant,
    val revokedAt: Instant?,
    val replacedBy: UUID?,
) {
    val consumed: Boolean get() = revokedAt != null || replacedBy != null
}

data class AccessToken(
    val token: String,
    val expiresInSeconds: Long,
)
