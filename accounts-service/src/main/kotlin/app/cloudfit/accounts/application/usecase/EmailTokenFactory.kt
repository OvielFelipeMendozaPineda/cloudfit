package app.cloudfit.accounts.application.usecase

import app.cloudfit.accounts.application.port.output.EmailTokenRepository
import app.cloudfit.accounts.domain.EmailToken
import app.cloudfit.accounts.domain.EmailTokenPurpose
import app.cloudfit.accounts.domain.OpaqueTokens
import app.cloudfit.shared.application.port.ClockProvider
import java.time.Duration
import java.util.UUID

class EmailTokenFactory(
    private val emailTokens: EmailTokenRepository,
    private val clock: ClockProvider,
) {
    suspend fun issue(userId: UUID, purpose: EmailTokenPurpose, ttl: Duration, passwordHash: String? = null): String {
        val plain = OpaqueTokens.generate()
        emailTokens.create(
            EmailToken(
                id = UUID.randomUUID(),
                userId = userId,
                tokenHash = OpaqueTokens.hash(plain),
                purpose = purpose,
                passwordHash = passwordHash,
                expiresAt = clock.now().plus(ttl),
                usedAt = null,
            ),
        )
        return plain
    }
}
