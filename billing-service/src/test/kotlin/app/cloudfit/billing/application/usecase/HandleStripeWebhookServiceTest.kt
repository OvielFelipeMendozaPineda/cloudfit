package app.cloudfit.billing.application.usecase

import app.cloudfit.billing.domain.CreditBucket
import app.cloudfit.billing.domain.Currency
import app.cloudfit.billing.domain.LedgerReason
import app.cloudfit.billing.domain.Payment
import app.cloudfit.billing.domain.PaymentProvider
import app.cloudfit.billing.domain.PaymentStatus
import app.cloudfit.billing.domain.PaymentsMode
import app.cloudfit.billing.domain.SubscriptionStatus
import app.cloudfit.billing.infrastructure.adapter.output.payment.StripeWebhookVerifier
import app.cloudfit.billing.support.BillingTestKit
import app.cloudfit.shared.application.error.ValidationException
import app.cloudfit.shared.testing.PassthroughTransactionRunner
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.util.UUID

class HandleStripeWebhookServiceTest : StringSpec({
    val secret = "whsec_test_secret"

    fun service(kit: BillingTestKit) = HandleStripeWebhookService(
        parser = StripeWebhookVerifier(secret, kit.clock),
        events = kit.webhookEvents,
        payments = kit.payments,
        subscriptions = kit.subscriptions,
        customers = kit.customers,
        book = kit.book,
        planGranter = kit.planGranter,
        policy = kit.policy,
        tx = PassthroughTransactionRunner,
    )

    fun signed(kit: BillingTestKit, payload: String) =
        StripeWebhookVerifier.signatureHeader(secret, payload, kit.clock.now().epochSecond)

    fun pendingPack(kit: BillingTestKit, user: UUID): Payment = Payment(
        UUID.randomUUID(), user, PaymentProvider.STRIPE, "PACK_10", 499, Currency.USD, PaymentStatus.PENDING, "cs_test_1",
    ).also { kit.payments.payments[it.id] = it }

    fun checkoutCompleted(eventId: String, payment: Payment) = """
        {"id":"$eventId","type":"checkout.session.completed","data":{"object":{
          "id":"cs_test_1","mode":"payment","payment_status":"paid","customer":"cus_1",
          "metadata":{"paymentId":"${payment.id}","userId":"${payment.userId}","productCode":"PACK_10"}}}}
    """.trimIndent()

    "a signed checkout.session.completed credits the pack exactly once" {
        val kit = BillingTestKit(PaymentsMode.LIVE)
        val user = UUID.randomUUID()
        val payment = pendingPack(kit, user)
        val payload = checkoutCompleted("evt_1", payment)
        val handler = service(kit)

        handler.execute(payload, signed(kit, payload))
        handler.execute(payload, signed(kit, payload))
        val retriedWithNewId = checkoutCompleted("evt_2", payment)
        handler.execute(retriedWithNewId, signed(kit, retriedWithNewId))

        kit.wallet.balance(user, CreditBucket.PACK) shouldBe 10
        kit.ledger.entries.count { it.reason == LedgerReason.PURCHASE } shouldBe 1
        kit.payments.payments.getValue(payment.id).status shouldBe PaymentStatus.PAID
    }

    "an invalid signature is rejected and grants nothing" {
        val kit = BillingTestKit(PaymentsMode.LIVE)
        val user = UUID.randomUUID()
        val payload = checkoutCompleted("evt_bad", pendingPack(kit, user))

        shouldThrow<ValidationException> { service(kit).execute(payload, "t=${kit.clock.now().epochSecond},v1=deadbeef") }
        shouldThrow<ValidationException> { service(kit).execute(payload, null) }
        shouldThrow<ValidationException> {
            service(kit).execute(payload, StripeWebhookVerifier.signatureHeader("whsec_other", payload, kit.clock.now().epochSecond))
        }

        kit.wallet.balance(user, CreditBucket.PACK) shouldBe 0
        kit.webhookEvents.events shouldBe emptySet()
    }

    "a signature outside the tolerance window is rejected" {
        val kit = BillingTestKit(PaymentsMode.LIVE)
        val payload = checkoutCompleted("evt_old", pendingPack(kit, UUID.randomUUID()))
        val header = StripeWebhookVerifier.signatureHeader(secret, payload, kit.clock.now().minus(Duration.ofHours(1)).epochSecond)

        shouldThrow<ValidationException> { service(kit).execute(payload, header) }
    }

    "invoice.paid renews the plan: expires old PLAN credits and grants the new period" {
        val kit = BillingTestKit(PaymentsMode.LIVE)
        val user = UUID.randomUUID()
        kit.customers.saveStripeCustomer(user, "cus_9")
        val periodEnd = kit.clock.now().plus(Duration.ofDays(30)).epochSecond

        fun invoice(eventId: String, invoiceId: String) = """
            {"id":"$eventId","type":"invoice.paid","data":{"object":{
              "id":"$invoiceId","customer":"cus_9","subscription":"sub_9",
              "subscription_details":{"metadata":{"planCode":"PLUS"}},
              "lines":{"data":[{"price":{"id":"price_plus"},"period":{"end":$periodEnd}}]}}}}
        """.trimIndent()

        val handler = service(kit)
        val first = invoice("evt_i1", "in_1")
        handler.execute(first, signed(kit, first))
        kit.wallet.balance(user, CreditBucket.PLAN) shouldBe 40

        val second = invoice("evt_i2", "in_2")
        handler.execute(second, signed(kit, second))
        handler.execute(second, signed(kit, second))

        kit.wallet.balance(user, CreditBucket.PLAN) shouldBe 40
        kit.ledger.entries.count { it.reason == LedgerReason.PLAN_GRANT } shouldBe 2
        kit.ledger.entries.single { it.reason == LedgerReason.PLAN_EXPIRE }.delta shouldBe -40
        kit.subscriptions.findByProviderId("sub_9")?.status shouldBe SubscriptionStatus.ACTIVE
        kit.walletSummary.execute(user).plan?.code shouldBe "PLUS"
    }

    "customer.subscription.deleted cancels the plan and expires PLAN credits" {
        val kit = BillingTestKit(PaymentsMode.LIVE)
        val user = UUID.randomUUID()
        kit.customers.saveStripeCustomer(user, "cus_7")
        val periodEnd = kit.clock.now().plus(Duration.ofDays(30)).epochSecond
        val invoice = """
            {"id":"evt_a","type":"invoice.paid","data":{"object":{"id":"in_a","customer":"cus_7","subscription":"sub_7",
              "lines":{"data":[{"price":{"id":"price_plus"},"period":{"end":$periodEnd}}]}}}}
        """.trimIndent()
        val deleted = """
            {"id":"evt_b","type":"customer.subscription.deleted","data":{"object":{"id":"sub_7","customer":"cus_7",
              "status":"canceled","cancel_at_period_end":false,"items":{"data":[{"price":{"id":"price_plus"}}]}}}}
        """.trimIndent()
        val handler = service(kit)

        handler.execute(invoice, signed(kit, invoice))
        handler.execute(deleted, signed(kit, deleted))

        kit.wallet.balance(user, CreditBucket.PLAN) shouldBe 0
        kit.walletSummary.execute(user).plan shouldBe null
    }

    "unknown events are acknowledged without side effects" {
        val kit = BillingTestKit(PaymentsMode.LIVE)
        val payload = """{"id":"evt_x","type":"charge.refunded","data":{"object":{}}}"""

        service(kit).execute(payload, signed(kit, payload))

        kit.ledger.entries shouldBe emptyList()
    }
})
