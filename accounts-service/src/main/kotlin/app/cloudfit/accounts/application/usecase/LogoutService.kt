package app.cloudfit.accounts.application.usecase

import app.cloudfit.accounts.application.port.input.LogoutUseCase
import app.cloudfit.accounts.application.port.output.RefreshTokenRepository
import app.cloudfit.accounts.domain.OpaqueTokens
import app.cloudfit.shared.application.port.ClockProvider
import app.cloudfit.shared.application.port.TransactionRunner

class LogoutService(
    private val refreshTokens: RefreshTokenRepository,
    private val tx: TransactionRunner,
    private val clock: ClockProvider,
) : LogoutUseCase {
    override suspend fun execute(refreshToken: String?) {
        if (refreshToken.isNullOrBlank()) return
        tx.inTransaction {
            refreshTokens.lockByHash(OpaqueTokens.hash(refreshToken))?.let {
                refreshTokens.revokeFamily(it.familyId, clock.now())
            }
        }
    }
}
