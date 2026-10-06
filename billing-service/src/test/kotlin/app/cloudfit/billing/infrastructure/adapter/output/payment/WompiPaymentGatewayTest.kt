package app.cloudfit.billing.infrastructure.adapter.output.payment

import app.cloudfit.billing.application.port.output.CheckoutRequest
import app.cloudfit.billing.domain.Currency
import app.cloudfit.billing.domain.ProductKind
import app.cloudfit.shared.application.error.ProviderNotConfiguredException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.util.UUID

class WompiPaymentGatewayTest : StringSpec({
    val config = WompiConfig("pub_test_abc", "test_integrity", "test_events", "sandbox", "https://checkout.wompi.co/p/", "https://app.test")

    "builds a web checkout URL with the integrity signature" {
        val gateway = WompiPaymentGateway(config)
        val paymentId = UUID.fromString("00000000-0000-0000-0000-000000000001")

        val session = gateway.createCheckout(
            CheckoutRequest(paymentId, UUID.randomUUID(), ProductKind.PACK, "PACK_10", "10 credits", 1_990_000, Currency.COP, null, null),
        )

        val expected = Crypto.sha256Hex("${paymentId}1990000COPtest_integrity")
        session.url shouldContain "public-key=pub_test_abc"
        session.url shouldContain "amount-in-cents=1990000"
        session.url shouldContain "reference=$paymentId"
        session.url shouldContain expected
        gateway.integritySignature(paymentId.toString(), 1_990_000, "COP") shouldBe expected
    }

    "is not configured without keys" {
        val gateway = WompiPaymentGateway(config.copy(publicKey = ""))
        gateway.configured shouldBe false
        shouldThrow<ProviderNotConfiguredException> {
            gateway.createCheckout(
                CheckoutRequest(UUID.randomUUID(), UUID.randomUUID(), ProductKind.PACK, "PACK_10", "x", 1, Currency.COP, null, null),
            )
        }
    }
})
