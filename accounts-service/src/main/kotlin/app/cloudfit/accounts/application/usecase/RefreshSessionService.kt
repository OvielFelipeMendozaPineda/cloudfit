package app.cloudfit.accounts.application.usecase

import app.cloudfit.accounts.application.port.input.RefreshSessionUseCase
import app.cloudfit.accounts.application.port.output.RefreshTokenRepository
import app.cloudfit.accounts.application.port.output.UserRepository
import app.cloudfit.accounts.domain.AccountsPolicy
import app.cloudfit.accounts.domain.OpaqueTokens
import app.cloudfit.accounts.domain.RefreshToken
import app.cloudfit.accounts.domain.Session
import app.cloudfit.shared.application.error.ErrorCodes
import app.cloudfit.shared.application.error.UnauthorizedException
import app.cloudfit.shared.application.port.ClockProvider
import app.cloudfit.shared.application.port.TransactionRunner
import java.time.Instant
import org.slf4j.LoggerFactory

class RefreshSessionService(
    private val refreshTokens: RefreshTokenRepository,
    private val users: UserRepository,
    private val sessions: SessionIssuer,
    private val tx: TransactionRunner,
    private val clock: ClockProvider,
    private val policy: AccountsPolicy,
) : RefreshSessionUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun execute(refreshToken: String?): Session {
        if (refreshToken.isNullOrBlank()) throw invalidToken()
        val session = tx.inTransaction {
            val now = clock.now()
            val token = refreshTokens.lockByHash(OpaqueTokens.hash(refreshToken)) ?: return@inTransaction null
            if (token.consumed && isConcurrentRefresh(token, now)) {
                val user = users.findById(token.userId) ?: return@inTransaction null
                return@inTransaction sessions.branch(user, token.familyId)
            }
            if (token.consumed) {
                log.warn("Refresh token reuse detected, revoking family {}", token.familyId)
                refreshTokens.revokeFamily(token.familyId, now)
                return@inTransaction null
            }
            if (!token.expiresAt.isAfter(now)) return@inTransaction null
            val user = users.findById(token.userId) ?: return@inTransaction null
            sessions.rotate(user, token)
        }
        return session ?: throw invalidToken()
    }

    private suspend fun isConcurrentRefresh(token: RefreshToken, now: Instant): Boolean {
        val replacedAt = token.revokedAt ?: return false
        if (token.replacedBy == null) return false
        if (replacedAt.plus(policy.refreshReuseGrace).isBefore(now)) return false
        return refreshTokens.isFamilyActive(token.familyId, now)
    }

    private fun invalidToken() = UnauthorizedException("Invalid refresh token", ErrorCodes.INVALID_TOKEN)
}
