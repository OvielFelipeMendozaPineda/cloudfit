package app.cloudfit.billing.application.usecase

import app.cloudfit.billing.application.port.input.GrantCreditsCommand
import app.cloudfit.billing.domain.CreditBucket
import app.cloudfit.billing.domain.HoldStatus
import app.cloudfit.billing.domain.LedgerReason
import app.cloudfit.billing.support.BillingTestKit
import app.cloudfit.shared.application.error.InsufficientCreditsException
import app.cloudfit.shared.testing.PassthroughTransactionRunner
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.util.UUID

class CreditOperationsTest : StringSpec({
    val tx = PassthroughTransactionRunner

    fun kit() = BillingTestKit()

    suspend fun BillingTestKit.grant(userId: UUID, bucket: CreditBucket, amount: Int, key: String = UUID.randomUUID().toString()) =
        GrantCreditsService(book, tx).execute(GrantCreditsCommand(userId, bucket, amount, LedgerReason.PURCHASE, key))

    "grant is idempotent by idempotency key" {
        val kit = kit()
        val user = UUID.randomUUID()
        val service = GrantCreditsService(kit.book, tx)
        val command = GrantCreditsCommand(user, CreditBucket.FREE, 5, LedgerReason.SIGNUP_BONUS, "signup:$user")

        service.execute(command) shouldBe true
        service.execute(command) shouldBe false

        kit.wallet.balance(user, CreditBucket.FREE) shouldBe 5
        kit.ledger.entries.size shouldBe 1
    }

    "reserve consumes buckets in order PLAN, FREE, REWARD, PACK" {
        val kit = kit()
        val user = UUID.randomUUID()
        kit.grant(user, CreditBucket.PACK, 1)
        kit.grant(user, CreditBucket.REWARD, 1)
        kit.grant(user, CreditBucket.FREE, 1)
        kit.grant(user, CreditBucket.PLAN, 1)
        val reserve = ReserveCreditsService(kit.book, kit.holds, tx)

        val consumed = (1..4).map { i ->
            reserve.execute(user, "look:$i")
            kit.ledger.findByRef("look:$i", LedgerReason.LOOK_RESERVED).single().bucket
        }

        consumed shouldBe listOf(CreditBucket.PLAN, CreditBucket.FREE, CreditBucket.REWARD, CreditBucket.PACK)
        CreditBucket.entries.forEach { kit.wallet.balance(user, it) shouldBe 0 }
    }

    "reserve skips an expired PLAN bucket" {
        val kit = kit()
        val user = UUID.randomUUID()
        GrantCreditsService(kit.book, tx).execute(
            GrantCreditsCommand(user, CreditBucket.PLAN, 10, LedgerReason.PLAN_GRANT, "plan-1", expiresAt = kit.clock.now().plusSeconds(60)),
        )
        kit.grant(user, CreditBucket.PACK, 2)
        kit.clock.advance(Duration.ofMinutes(5))

        ReserveCreditsService(kit.book, kit.holds, tx).execute(user, "look:x")

        kit.ledger.findByRef("look:x", LedgerReason.LOOK_RESERVED).single().bucket shouldBe CreditBucket.PACK
        kit.walletSummary.execute(user).balance.plan shouldBe 0
    }

    "reserve without credits throws INSUFFICIENT_CREDITS and changes nothing" {
        val kit = kit()
        val user = UUID.randomUUID()

        shouldThrow<InsufficientCreditsException> {
            ReserveCreditsService(kit.book, kit.holds, tx).execute(user, "look:none")
        }

        kit.ledger.entries shouldBe emptyList()
        kit.holds.holds shouldBe emptyMap()
    }

    "refund returns credits to the bucket they came from, only once" {
        val kit = kit()
        val user = UUID.randomUUID()
        kit.grant(user, CreditBucket.FREE, 1)
        kit.grant(user, CreditBucket.PACK, 3)
        ReserveCreditsService(kit.book, kit.holds, tx).execute(user, "look:r")
        val refund = RefundCreditsService(kit.book, kit.holds, kit.ledger, tx)

        refund.execute("look:r") shouldBe true
        refund.execute("look:r") shouldBe false

        kit.wallet.balance(user, CreditBucket.FREE) shouldBe 1
        kit.wallet.balance(user, CreditBucket.PACK) shouldBe 3
        kit.holds.holds.getValue("look:r").status shouldBe HoldStatus.REFUNDED
        kit.ledger.findByRef("look:r", LedgerReason.LOOK_REFUND).single().bucket shouldBe CreditBucket.FREE
    }

    "confirmed holds are never refunded" {
        val kit = kit()
        val user = UUID.randomUUID()
        kit.grant(user, CreditBucket.FREE, 1)
        ReserveCreditsService(kit.book, kit.holds, tx).execute(user, "look:c")

        ConfirmCreditsService(kit.holds, tx).execute("look:c")
        RefundCreditsService(kit.book, kit.holds, kit.ledger, tx).execute("look:c") shouldBe false

        kit.wallet.balance(user, CreditBucket.FREE) shouldBe 0
        kit.holds.holds.getValue("look:c").status shouldBe HoldStatus.CONFIRMED
    }

    "ledger lists most recent entries first with a bounded limit" {
        val kit = kit()
        val user = UUID.randomUUID()
        repeat(3) {
            kit.grant(user, CreditBucket.PACK, 1)
            kit.clock.advance(Duration.ofSeconds(1))
        }

        val entries = GetLedgerService(kit.ledger).execute(user, 2)

        entries.size shouldBe 2
        entries.first().createdAt.isAfter(entries.last().createdAt) shouldBe true
    }
})
