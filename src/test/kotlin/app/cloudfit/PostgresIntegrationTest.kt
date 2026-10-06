package app.cloudfit

import app.cloudfit.accounts.domain.User
import app.cloudfit.accounts.infrastructure.adapter.output.persistence.PostgresUserRepository
import app.cloudfit.billing.application.port.input.GrantCreditsCommand
import app.cloudfit.billing.application.usecase.ConfirmCreditsService
import app.cloudfit.billing.application.usecase.CreditBook
import app.cloudfit.billing.application.usecase.GetWalletSummaryService
import app.cloudfit.billing.application.usecase.GrantCreditsService
import app.cloudfit.billing.application.usecase.RefundCreditsService
import app.cloudfit.billing.application.usecase.ReserveCreditsService
import app.cloudfit.billing.domain.CreditBucket
import app.cloudfit.billing.domain.LedgerReason
import app.cloudfit.billing.infrastructure.adapter.output.persistence.PostgresCreditHoldRepository
import app.cloudfit.billing.infrastructure.adapter.output.persistence.PostgresLedgerRepository
import app.cloudfit.billing.infrastructure.adapter.output.persistence.PostgresSubscriptionRepository
import app.cloudfit.billing.infrastructure.adapter.output.persistence.PostgresWalletRepository
import app.cloudfit.shared.application.error.InsufficientCreditsException
import app.cloudfit.shared.domain.AppLocale
import app.cloudfit.shared.domain.ClothingCategory
import app.cloudfit.shared.infrastructure.database.DatabaseConfig
import app.cloudfit.shared.infrastructure.database.DatabaseFactory
import app.cloudfit.shared.infrastructure.database.ExposedTransactionRunner
import app.cloudfit.shared.infrastructure.time.SystemClockProvider
import app.cloudfit.styling.application.usecase.CreateLookService
import app.cloudfit.styling.domain.StylingPolicy
import app.cloudfit.styling.domain.WardrobeItem
import app.cloudfit.styling.domain.WardrobeSnapshot
import app.cloudfit.styling.infrastructure.adapter.output.persistence.PostgresLookRepository
import app.cloudfit.wiring.CreditWalletAdapter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jetbrains.exposed.sql.transactions.transaction
import org.testcontainers.DockerClientFactory
import org.testcontainers.postgresql.PostgreSQLContainer

private val dockerAvailable: Boolean = runCatching { DockerClientFactory.instance().isDockerAvailable }.getOrDefault(false)

class PostgresIntegrationTest : StringSpec({
    val postgres = PostgreSQLContainer("postgres:17-alpine")
    val clock = SystemClockProvider()
    val tx = ExposedTransactionRunner()
    val users = PostgresUserRepository(clock)
    val wallet = PostgresWalletRepository(clock)
    val ledger = PostgresLedgerRepository()
    val holds = PostgresCreditHoldRepository(clock)
    val book = CreditBook(wallet, ledger, clock)
    val grant = GrantCreditsService(book, tx)
    val reserve = ReserveCreditsService(book, holds, tx)
    val refund = RefundCreditsService(book, holds, ledger, tx)
    val summary = GetWalletSummaryService(wallet, PostgresSubscriptionRepository(clock), clock)

    suspend fun newUser(): UUID {
        val id = UUID.randomUUID()
        users.create(User(id, "u-$id@example.com", null, clock.now(), null, AppLocale.EN, clock.now()))
        return id
    }

    beforeSpec {
        if (dockerAvailable) {
            postgres.start()
            DatabaseFactory.connect(
                DatabaseConfig(postgres.host, postgres.firstMappedPort, postgres.databaseName, postgres.username, postgres.password, 10),
            )
        }
    }

    afterSpec { if (dockerAvailable) postgres.stop() }

    "flyway migrates and email uniqueness is case-insensitive".config(enabled = dockerAvailable) {
        val id = newUser()
        users.findByEmail("U-$id@EXAMPLE.com")?.id shouldBe id
        shouldThrow<Exception> { users.create(User(UUID.randomUUID(), "U-$id@Example.COM", null, null, null, AppLocale.EN, clock.now())) }
    }

    "grants are idempotent and the ledger is append-only".config(enabled = dockerAvailable) {
        val user = newUser()
        val command = GrantCreditsCommand(user, CreditBucket.FREE, 5, LedgerReason.SIGNUP_BONUS, "signup:$user")

        grant.execute(command) shouldBe true
        grant.execute(command) shouldBe false
        summary.execute(user).balance.free shouldBe 5

        shouldThrow<Exception> { transaction { exec("UPDATE credit_ledger SET delta = 99 WHERE user_id = '$user'") } }
    }

    "concurrent reservations never overspend thanks to row locks".config(enabled = dockerAvailable) {
        val user = newUser()
        grant.execute(GrantCreditsCommand(user, CreditBucket.PACK, 3, LedgerReason.PURCHASE, "pack:$user"))

        val outcomes = coroutineScope {
            (1..8).map { i -> async { runCatching { reserve.execute(user, "look:$user:$i") }.isSuccess } }.awaitAll()
        }

        outcomes.count { it } shouldBe 3
        summary.execute(user).balance.total shouldBe 0
    }

    "refunds restore the original bucket once, confirmed holds stay debited".config(enabled = dockerAvailable) {
        val user = newUser()
        grant.execute(GrantCreditsCommand(user, CreditBucket.FREE, 1, LedgerReason.SIGNUP_BONUS, "signup:$user"))
        grant.execute(GrantCreditsCommand(user, CreditBucket.PACK, 1, LedgerReason.PURCHASE, "pack:$user"))

        reserve.execute(user, "a:$user")
        reserve.execute(user, "b:$user")
        ConfirmCreditsService(holds, tx).execute("b:$user")
        refund.execute("a:$user") shouldBe true
        refund.execute("a:$user") shouldBe false
        refund.execute("b:$user") shouldBe false

        val balance = summary.execute(user).balance
        balance.free shouldBe 1
        balance.pack shouldBe 0
    }

    "a look and its credit reservation commit or roll back together".config(enabled = dockerAvailable) {
        val user = newUser()
        val looks = PostgresLookRepository(clock)
        val wardrobe = WardrobeSnapshot(
            listOf(
                WardrobeItem(UUID.randomUUID(), ClothingCategory.DRESS, "/d.png"),
                WardrobeItem(UUID.randomUUID(), ClothingCategory.SHOES, "/s.png"),
            ),
            null,
        )
        val scheduled = mutableListOf<UUID>()
        val create = CreateLookService(
            looks = looks,
            wardrobe = { wardrobe },
            wallet = CreditWalletAdapter(reserve, ConfirmCreditsService(holds, tx), refund),
            scheduler = { scheduled += it },
            policy = StylingPolicy(),
            tx = tx,
            clock = clock,
        )

        shouldThrow<InsufficientCreditsException> { create.execute(user, "brunch") }
        looks.list(user, savedOnly = false, limit = 10) shouldBe emptyList()

        grant.execute(GrantCreditsCommand(user, CreditBucket.FREE, 1, LedgerReason.SIGNUP_BONUS, "signup:$user"))
        val look = create.execute(user, "brunch")

        looks.findById(look.id)?.event shouldBe "brunch"
        scheduled shouldBe listOf(look.id)
        summary.execute(user).balance.total shouldBe 0
    }
})
