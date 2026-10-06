package app.cloudfit.billing.application.usecase

import app.cloudfit.billing.application.port.input.HandleWompiWebhookUseCase
import app.cloudfit.billing.application.port.output.PaymentRepository
import app.cloudfit.billing.application.port.output.WebhookEventRepository
import app.cloudfit.billing.application.port.output.WompiWebhookParser
import app.cloudfit.billing.domain.BillingPolicy
import app.cloudfit.billing.domain.CreditBucket
import app.cloudfit.billing.domain.LedgerReason
import app.cloudfit.billing.domain.PaymentProvider
import app.cloudfit.billing.domain.PaymentStatus
import app.cloudfit.shared.application.port.TransactionRunner
import org.slf4j.LoggerFactory

class HandleWompiWebhookService(
    private val parser: WompiWebhookParser,
    private val events: WebhookEventRepository,
    private val payments: PaymentRepository,
    private val book: CreditBook,
    private val policy: BillingPolicy,
    private val tx: TransactionRunner,
) : HandleWompiWebhookUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun execute(payload: String, checksumHeader: String?) {
        val event = parser.parse(payload, checksumHeader) ?: return
        tx.inTransaction {
            val eventId = "${event.transactionId}:${event.status}"
            if (!events.registerIfNew(PaymentProvider.WOMPI, eventId, "transaction.updated")) return@inTransaction
            val payment = event.reference.toUuidOrNull()?.let { payments.findById(it) }
            if (payment == null || payment.provider != PaymentProvider.WOMPI) {
                log.warn("Wompi transaction {} has no matching payment", event.transactionId)
                return@inTransaction
            }
            when (event.status.uppercase()) {
                "APPROVED" -> approve(payment, event.transactionId, event.amountInCents, event.currency)
                "DECLINED", "ERROR", "VOIDED" ->
                    if (payment.status == PaymentStatus.PENDING) payments.update(payment.id, PaymentStatus.FAILED, event.transactionId)
            }
        }
    }

    private suspend fun approve(payment: app.cloudfit.billing.domain.Payment, transactionId: String, amount: Long, currency: String) {
        if (payment.amount != amount || !payment.currency.name.equals(currency, ignoreCase = true)) {
            log.warn("Wompi transaction {} amount mismatch for payment {}", transactionId, payment.id)
            return
        }
        val pack = policy.catalog.pack(payment.productCode) ?: return log.warn("Unknown pack {}", payment.productCode)
        book.grant(
            buckets = book.lock(payment.userId),
            userId = payment.userId,
            bucket = CreditBucket.PACK,
            amount = pack.credits,
            reason = LedgerReason.PURCHASE,
            idempotencyKey = "wompi:$transactionId",
            refId = payment.id.toString(),
        )
        payments.update(payment.id, PaymentStatus.PAID, transactionId)
    }
}
