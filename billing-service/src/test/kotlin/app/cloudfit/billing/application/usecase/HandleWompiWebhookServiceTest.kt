package app.cloudfit.billing.application.usecase

import app.cloudfit.billing.domain.CreditBucket
import app.cloudfit.billing.domain.Currency
import app.cloudfit.billing.domain.Payment
import app.cloudfit.billing.domain.PaymentProvider
import app.cloudfit.billing.domain.PaymentStatus
import app.cloudfit.billing.domain.PaymentsMode
import app.cloudfit.billing.infrastructure.adapter.output.payment.WompiWebhookVerifier
import app.cloudfit.billing.support.BillingTestKit
import app.cloudfit.shared.application.error.ValidationException
import app.cloudfit.shared.testing.PassthroughTransactionRunner
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.security.MessageDigest
import java.util.UUID

class HandleWompiWebhookServiceTest : StringSpec({
    val secret = "test_events_secret"

    fun service(kit: BillingTestKit) = HandleWompiWebhookService(
        parser = WompiWebhookVerifier(secret),
        events = kit.webhookEvents,
        payments = kit.payments,
        book = kit.book,
        policy = kit.policy,
        tx = PassthroughTransactionRunner,
    )

    fun sha256(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    fun event(payment: Payment, transactionId: String, status: String, amount: Long = payment.amount, signSecret: String = secret): String {
        val timestamp = 1_791_000_000L
        val checksum = sha256("$transactionId$status$amount$timestamp$signSecret")
        return """
            {"event":"transaction.updated","data":{"transaction":{
              "id":"$transactionId","status":"$status","amount_in_cents":$amount,"reference":"${payment.id}","currency":"COP"}},
             "environment":"test","signature":{"properties":["transaction.id","transaction.status","transaction.amount_in_cents"],
             "checksum":"$checksum"},"timestamp":$timestamp,"sent_at":"2026-10-06T12:00:00Z"}
        """.trimIndent()
    }

    fun pending(kit: BillingTestKit, user: UUID) = Payment(
        UUID.randomUUID(), user, PaymentProvider.WOMPI, "PACK_10", 1_990_000, Currency.COP, PaymentStatus.PENDING, null,
    ).also { kit.payments.payments[it.id] = it }

    "an APPROVED transaction with a valid checksum credits the pack exactly once" {
        val kit = BillingTestKit(PaymentsMode.LIVE)
        val user = UUID.randomUUID()
        val payment = pending(kit, user)
        val payload = event(payment, "tx_1", "APPROVED")

        service(kit).execute(payload, null)
        service(kit).execute(payload, null)

        kit.wallet.balance(user, CreditBucket.PACK) shouldBe 10
        kit.payments.payments.getValue(payment.id).status shouldBe PaymentStatus.PAID
        kit.payments.payments.getValue(payment.id).providerRef shouldBe "tx_1"
    }

    "an invalid checksum is rejected" {
        val kit = BillingTestKit(PaymentsMode.LIVE)
        val user = UUID.randomUUID()
        val payload = event(pending(kit, user), "tx_2", "APPROVED", signSecret = "forged")

        shouldThrow<ValidationException> { service(kit).execute(payload, null) }
        kit.wallet.balance(user, CreditBucket.PACK) shouldBe 0
    }

    "a tampered amount breaks the checksum" {
        val kit = BillingTestKit(PaymentsMode.LIVE)
        val payment = pending(kit, UUID.randomUUID())
        val payload = event(payment, "tx_3", "APPROVED").replace("\"amount_in_cents\":1990000", "\"amount_in_cents\":100")

        shouldThrow<ValidationException> { service(kit).execute(payload, null) }
    }

    "a signed transaction with a different amount than the payment grants nothing" {
        val kit = BillingTestKit(PaymentsMode.LIVE)
        val user = UUID.randomUUID()
        val payment = pending(kit, user)

        service(kit).execute(event(payment, "tx_4", "APPROVED", amount = 100), null)

        kit.wallet.balance(user, CreditBucket.PACK) shouldBe 0
    }

    "a DECLINED transaction marks the payment as failed" {
        val kit = BillingTestKit(PaymentsMode.LIVE)
        val user = UUID.randomUUID()
        val payment = pending(kit, user)

        service(kit).execute(event(payment, "tx_5", "DECLINED"), null)

        kit.payments.payments.getValue(payment.id).status shouldBe PaymentStatus.FAILED
        kit.wallet.balance(user, CreditBucket.PACK) shouldBe 0
    }
})
