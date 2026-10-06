package app.cloudfit.billing.application.usecase

import app.cloudfit.billing.domain.CreditBucket
import app.cloudfit.billing.domain.PaymentProvider
import app.cloudfit.billing.domain.Subscription
import app.cloudfit.billing.domain.SubscriptionStatus
import app.cloudfit.billing.support.BillingTestKit
import app.cloudfit.shared.application.error.ForbiddenException
import app.cloudfit.shared.application.error.RateLimitedException
import app.cloudfit.shared.application.error.ValidationException
import app.cloudfit.shared.testing.PassthroughTransactionRunner
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.util.UUID

class ClaimAdRewardServiceTest : StringSpec({

    fun service(kit: BillingTestKit) =
        ClaimAdRewardService(kit.book, kit.ledger, kit.walletSummary, kit.policy, PassthroughTransactionRunner, kit.clock)

    "grants one REWARD credit up to three times per day" {
        val kit = BillingTestKit()
        val user = UUID.randomUUID()
        val claim = service(kit)

        (1..3).map { claim.execute(user, "home") } shouldBe listOf(1, 2, 3)
        shouldThrow<RateLimitedException> { claim.execute(user, "home") }
        kit.wallet.balance(user, CreditBucket.REWARD) shouldBe 3

        kit.clock.advance(Duration.ofDays(1))
        claim.execute(user, "home") shouldBe 4
    }

    "is forbidden while the user has an active plan" {
        val kit = BillingTestKit()
        val user = UUID.randomUUID()
        kit.subscriptions.save(
            Subscription(
                UUID.randomUUID(), user, PaymentProvider.STRIPE, "sub_1", "PLUS",
                SubscriptionStatus.ACTIVE, kit.clock.now().plus(Duration.ofDays(10)), false,
            ),
        )

        shouldThrow<ForbiddenException> { service(kit).execute(user, "home") }
    }

    "requires a placement" {
        shouldThrow<ValidationException> { service(BillingTestKit()).execute(UUID.randomUUID(), " ") }
    }
})
