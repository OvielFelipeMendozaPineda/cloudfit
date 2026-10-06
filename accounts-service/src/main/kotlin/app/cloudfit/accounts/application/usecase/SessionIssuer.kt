package app.cloudfit.accounts.application.usecase

import app.cloudfit.accounts.application.port.output.RefreshTokenRepository
import app.cloudfit.accounts.application.port.output.TokenIssuer
import app.cloudfit.accounts.domain.AccountsPolicy
import app.cloudfit.accounts.domain.OpaqueTokens
import app.cloudfit.accounts.domain.RefreshToken
import app.cloudfit.accounts.domain.Session
import app.cloudfit.accounts.domain.User
import app.cloudfit.shared.application.port.ClockProvider
import java.util.UUID

class SessionIssuer(
    private val refreshTokens: RefreshTokenRepository,
    private val tokenIssuer: TokenIssuer,
    private val meAssembler: MeAssembler,
    private val policy: AccountsPolicy,
    private val clock: ClockProvider,
) {
    suspend fun start(user: User): Session = issue(user, familyId = UUID.randomUUID(), previous = null)

    suspend fun rotate(user: User, previous: RefreshToken): Session = issue(user, previous.familyId, previous)

    suspend fun branch(user: User, familyId: UUID): Session = issue(user, familyId, previous = null)

    private suspend fun issue(user: User, familyId: UUID, previous: RefreshToken?): Session {
        val now = clock.now()
        val plain = OpaqueTokens.generate()
        val token = RefreshToken(
            id = UUID.randomUUID(),
            userId = user.id,
            familyId = familyId,
            tokenHash = OpaqueTokens.hash(plain),
            expiresAt = now.plus(policy.refreshTokenTtl),
            revokedAt = null,
            replacedBy = null,
        )
        refreshTokens.create(token)
        previous?.let { refreshTokens.markReplaced(it.id, token.id, now) }
        return Session(
            accessToken = tokenIssuer.issueAccessToken(user.id),
            refreshToken = plain,
            refreshExpiresAt = token.expiresAt,
            me = meAssembler.assemble(user),
        )
    }
}
