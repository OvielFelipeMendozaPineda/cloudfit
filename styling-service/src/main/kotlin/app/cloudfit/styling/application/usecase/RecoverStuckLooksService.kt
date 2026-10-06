package app.cloudfit.styling.application.usecase

import app.cloudfit.shared.application.port.ClockProvider
import app.cloudfit.styling.application.port.input.RecoverStuckLooksUseCase
import app.cloudfit.styling.application.port.output.CreditWallet
import app.cloudfit.styling.application.port.output.LookRepository
import app.cloudfit.styling.domain.LookFailureCodes
import app.cloudfit.styling.domain.StylingPolicy
import org.slf4j.LoggerFactory

class RecoverStuckLooksService(
    private val looks: LookRepository,
    private val wallet: CreditWallet,
    private val policy: StylingPolicy,
    private val clock: ClockProvider,
) : RecoverStuckLooksUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun execute(): Int {
        val cutoff = clock.now().minus(policy.stuckAfter)
        var recovered = 0
        looks.findStale(cutoff).forEach { look ->
            if (looks.failIfStale(look.id, LookFailureCodes.INTERRUPTED, cutoff)) {
                wallet.refund(look.id)
                recovered++
            }
        }
        if (recovered > 0) log.warn("Recovered {} stuck look(s)", recovered)
        return recovered
    }
}
