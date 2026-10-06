package app.cloudfit.wiring

import app.cloudfit.accounts.application.port.output.AccountBalanceReader
import app.cloudfit.accounts.application.port.output.AvatarUrlReader
import app.cloudfit.accounts.application.port.output.WardrobeLimitsReader
import app.cloudfit.accounts.application.port.output.WelcomeBonusGranter
import app.cloudfit.accounts.domain.AccountBalance
import app.cloudfit.accounts.domain.CreditsView
import app.cloudfit.accounts.domain.PlanView
import app.cloudfit.billing.application.port.input.GetWalletSummaryUseCase
import app.cloudfit.billing.application.port.input.GrantCreditsCommand
import app.cloudfit.billing.application.port.input.GrantCreditsUseCase
import app.cloudfit.billing.domain.CreditBucket
import app.cloudfit.billing.domain.LedgerReason
import app.cloudfit.wardrobe.application.port.input.GetAvatarUseCase
import app.cloudfit.wardrobe.application.port.input.GetWardrobeLimitsUseCase
import java.util.UUID

class WelcomeBonusAdapter(
    private val grantCredits: GrantCreditsUseCase,
    private val amount: Int,
) : WelcomeBonusGranter {
    override suspend fun grant(userId: UUID) {
        grantCredits.execute(
            GrantCreditsCommand(
                userId = userId,
                bucket = CreditBucket.FREE,
                amount = amount,
                reason = LedgerReason.SIGNUP_BONUS,
                idempotencyKey = "signup:$userId",
            ),
        )
    }
}

class AccountBalanceAdapter(private val walletSummary: GetWalletSummaryUseCase) : AccountBalanceReader {
    override suspend fun read(userId: UUID): AccountBalance {
        val summary = walletSummary.execute(userId)
        val balance = summary.balance
        return AccountBalance(
            credits = CreditsView(balance.total, balance.plan, balance.free, balance.pack, balance.reward),
            plan = summary.plan?.let { PlanView(it.code, it.status.name, it.renewsAt) },
        )
    }
}

class AvatarUrlAdapter(private val getAvatar: GetAvatarUseCase) : AvatarUrlReader {
    override suspend fun avatarUrl(userId: UUID): String? = getAvatar.execute(userId)
}

class WardrobeLimitsAdapter(private val limits: GetWardrobeLimitsUseCase) : WardrobeLimitsReader {
    override suspend fun maxClothes(userId: UUID): Int? = limits.execute(userId)
}
