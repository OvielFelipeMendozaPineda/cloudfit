package app.cloudfit.accounts.application.port.output

import app.cloudfit.accounts.domain.RefreshToken
import java.time.Instant
import java.util.UUID

interface RefreshTokenRepository {
    suspend fun create(token: RefreshToken)

    suspend fun lockByHash(tokenHash: String): RefreshToken?

    suspend fun markReplaced(id: UUID, replacedBy: UUID, at: Instant)

    suspend fun revokeFamily(familyId: UUID, at: Instant)

    suspend fun isFamilyActive(familyId: UUID, now: Instant): Boolean

    suspend fun revokeAllForUser(userId: UUID, at: Instant)
}
