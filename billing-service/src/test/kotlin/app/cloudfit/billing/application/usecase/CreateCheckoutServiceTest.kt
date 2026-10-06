package app.cloudfit.billing.application.usecase

import app.cloudfit.billing.application.port.input.CreateCheckoutCommand
import app.cloudfit.billing.domain.CreditBucket
import app.cloudfit.billing.domain.Currency
import app.cloudfit.billing.domain.PaymentProvider
import app.cloudfit.billing.domain.PaymentStatus
import app.cloudfit.billing.domain.PaymentsMode
import app.cloudfit.billing.support.BillingTestKit
import app.cloudfit.billing.support.FakeGateway
import app.cloudfit.billing.support.FakeStripeAccounts
import app.cloudfit.shared.application.error.ConflictException
import app.cloudfit.shared.application.error.ProviderNotConfiguredException
import app.cloudfit.shared.application.error.ValidationException
import app.cloudfit.shared.testing.PassthroughTransactionRunner
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import java.util.UUID

class CreateCheckoutServiceTest : StringSpec({

    fun service(kit: BillingTestKit, gateways: List<FakeGateway> = emptyList()) = CreateCheckoutService(
        policy = kit.policy,
        gateways = gateways,
        stripeAccounts = FakeStripeAccounts(),
        customers = kit.customers,
        users = { "ana@example.com" },
        payments = kit.payments,
        subscriptions = kit.subscriptions,
        walletSummary = kit.walletSummary,
        book = kit.book,
        planGranter = kit.planGranter,
        tx = PassthroughTransactionRunner,
        clock = kit.clock,
    )

    "fake mode credits a pack immediately" {
        val kit = BillingTestKit(PaymentsMode.FAKE)
        val user = UUID.randomUUID()

        service(kit).execute(CreateCheckoutCommand(user, "PACK_10", null, "US")) shouldBe "/billing/success?fake=1"

        kit.wallet.balance(user, CreditBucket.PACK) shouldBe 10
        kit.payments.payments.values.single().status shouldBe PaymentStatus.PAID
    }

    "fake mode activates a plan with expiring PLAN credits" {
        val kit = BillingTestKit(PaymentsMode.FAKE)
        val user = UUID.randomUUID()

        service(kit).execute(CreateCheckoutCommand(user, "PLUS", null, null))

        val summary = kit.walletSummary.execute(user)
        summary.plan?.code shouldBe "PLUS"
        summary.balance.plan shouldBe 40
        shouldThrow<ConflictException> { service(kit).execute(CreateCheckoutCommand(user, "PLUS", null, null)) }
    }

    "plans are not sold in Colombia" {
        val kit = BillingTestKit(PaymentsMode.FAKE)
        shouldThrow<ValidationException> { service(kit).execute(CreateCheckoutCommand(UUID.randomUUID(), "PLUS", null, "CO")) }
    }

    "unknown products are rejected" {
        shouldThrow<ValidationException> {
            service(BillingTestKit()).execute(CreateCheckoutCommand(UUID.randomUUID(), "NOPE", null, null))
        }
    }

    "live mode routes Colombia to Wompi in COP and records a pending payment" {
        val kit = BillingTestKit(PaymentsMode.LIVE)
        val wompi = FakeGateway(PaymentProvider.WOMPI)
        val user = UUID.randomUUID()

        val url = service(kit, listOf(wompi, FakeGateway(PaymentProvider.STRIPE))).execute(CreateCheckoutCommand(user, "PACK_10", null, "CO"))

        url shouldStartWith "https://pay.example/"
        wompi.requests shouldHaveSize 1
        wompi.requests.single().currency shouldBe Currency.COP
        wompi.requests.single().amount shouldBe 1_990_000L
        kit.payments.payments.values.single().status shouldBe PaymentStatus.PENDING
        kit.wallet.balance(user, CreditBucket.PACK) shouldBe 0
    }

    "live mode uses Stripe elsewhere and creates the Stripe customer once" {
        val kit = BillingTestKit(PaymentsMode.LIVE)
        val stripe = FakeGateway(PaymentProvider.STRIPE)
        val user = UUID.randomUUID()

        service(kit, listOf(stripe)).execute(CreateCheckoutCommand(user, "PLUS", null, "US"))

        stripe.requests.single().customerId shouldBe "cus_$user"
        kit.customers.customers[user] shouldBe "cus_$user"
    }

    "live mode without provider keys answers PROVIDER_NOT_CONFIGURED" {
        val kit = BillingTestKit(PaymentsMode.LIVE)
        shouldThrow<ProviderNotConfiguredException> {
            service(kit, listOf(FakeGateway(PaymentProvider.STRIPE, configured = false)))
                .execute(CreateCheckoutCommand(UUID.randomUUID(), "PACK_10", null, "US"))
        }
    }

    "catalog maps Colombia to Wompi packs only" {
        val catalog = GetCatalogService(BillingTestKit(PaymentsMode.LIVE).policy)

        val co = catalog.execute("co")
        co.provider shouldBe PaymentProvider.WOMPI
        co.currency shouldBe Currency.COP
        co.plans shouldBe emptyList()

        val us = catalog.execute(null)
        us.provider shouldBe PaymentProvider.STRIPE
        us.plans.single().code shouldBe "PLUS"
    }
})
