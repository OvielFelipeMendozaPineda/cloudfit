package app.cloudfit.styling.application.usecase

import app.cloudfit.shared.application.ai.AiCallContext
import app.cloudfit.shared.application.ai.GeneratedImage
import app.cloudfit.shared.application.error.ConflictException
import app.cloudfit.shared.application.error.ErrorCodes
import app.cloudfit.shared.application.error.InsufficientCreditsException
import app.cloudfit.shared.application.error.NotFoundException
import app.cloudfit.shared.application.error.UnprocessableException
import app.cloudfit.shared.application.error.ValidationException
import app.cloudfit.shared.domain.AppLocale
import app.cloudfit.shared.domain.ClothingCategory
import app.cloudfit.shared.testing.MutableClock
import app.cloudfit.shared.testing.PassthroughTransactionRunner
import app.cloudfit.styling.application.port.output.ClothePicker
import app.cloudfit.styling.application.port.output.OutfitImageRenderer
import app.cloudfit.styling.domain.LookFailureCodes
import app.cloudfit.styling.domain.LookStatus
import app.cloudfit.styling.domain.Pick
import app.cloudfit.styling.domain.StylingPolicy
import app.cloudfit.styling.domain.WardrobeItem
import app.cloudfit.styling.domain.WardrobeSnapshot
import app.cloudfit.styling.support.FakeCreditWallet
import app.cloudfit.styling.support.InMemoryLookRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.util.UUID
import kotlinx.coroutines.currentCoroutineContext

class LookLifecycleTest : StringSpec({

    class Fixture(
        var snapshot: WardrobeSnapshot,
        var picker: ClothePicker,
        var renderer: OutfitImageRenderer = OutfitImageRenderer { _, _ -> GeneratedImage(byteArrayOf(1), "image/png") },
        credits: Int = 1,
    ) {
        val clock = MutableClock()
        val looks = InMemoryLookRepository { clock.now() }
        val wallet = FakeCreditWallet(credits)
        val scheduled = mutableListOf<UUID>()
        val savedImages = mutableListOf<UUID>()
        val locales = mutableListOf<AppLocale>()
        val aiOperations = mutableListOf<String>()
        val policy = StylingPolicy()

        val create = CreateLookService(looks, { snapshot }, wallet, { scheduled += it }, policy, PassthroughTransactionRunner, clock)
        val process = ProcessLookService(
            looks = looks,
            wardrobe = { snapshot },
            locales = { AppLocale.ES },
            picker = { event, items, locale ->
                locales += locale
                currentCoroutineContext()[AiCallContext]?.let { aiOperations += it.operation }
                picker.pick(event, items, locale)
            },
            renderer = { items, avatar ->
                currentCoroutineContext()[AiCallContext]?.let { aiOperations += it.operation }
                renderer.render(items, avatar)
            },
            images = { userId, _, _ -> savedImages += userId; "/api/v1/images/${UUID.randomUUID()}.png" },
            wallet = wallet,
        )
        val recover = RecoverStuckLooksService(looks, wallet, policy, clock)
    }

    val top = WardrobeItem(UUID.randomUUID(), ClothingCategory.TOP, "/t.png")
    val bottom = WardrobeItem(UUID.randomUUID(), ClothingCategory.BOTTOM, "/b.png")
    val shoes = WardrobeItem(UUID.randomUUID(), ClothingCategory.SHOES, "/s.png")
    val dress = WardrobeItem(UUID.randomUUID(), ClothingCategory.DRESS, "/d.png")
    val complete = WardrobeSnapshot(listOf(top, bottom, shoes), avatarUrl = "/avatar.png")
    val okPicker = ClothePicker { _, _, _ -> Pick(listOf(top.id, bottom.id, shoes.id), "Fresco y elegante.") }

    "wardrobe completeness requires (top + bottom or dress) and shoes" {
        complete.isComplete shouldBe true
        WardrobeSnapshot(listOf(dress, shoes), null).isComplete shouldBe true
        WardrobeSnapshot(listOf(top, shoes), null).isComplete shouldBe false
        WardrobeSnapshot(listOf(top, bottom), null).isComplete shouldBe false
    }

    "creating a look reserves a credit, persists it QUEUED and schedules the job" {
        val f = Fixture(complete, okPicker)
        val user = UUID.randomUUID()

        val look = f.create.execute(user, "  boda en la playa ")

        look.status shouldBe LookStatus.QUEUED
        look.event shouldBe "boda en la playa"
        f.wallet.reserved shouldContainExactly listOf(look.id)
        f.scheduled shouldContainExactly listOf(look.id)
        GetLookService(f.looks).execute(user, look.id).status shouldBe LookStatus.QUEUED
    }

    "an incomplete wardrobe answers WARDROBE_INCOMPLETE without reserving" {
        val f = Fixture(WardrobeSnapshot(listOf(top), null), okPicker)
        shouldThrow<UnprocessableException> { f.create.execute(UUID.randomUUID(), "brunch") }.code shouldBe ErrorCodes.WARDROBE_INCOMPLETE
        f.wallet.reserved shouldBe emptyList()
    }

    "without credits the look is not created" {
        val f = Fixture(complete, okPicker, credits = 0)
        shouldThrow<InsufficientCreditsException> { f.create.execute(UUID.randomUUID(), "brunch") }
        f.looks.looks shouldBe emptyMap()
        f.scheduled shouldBe emptyList()
    }

    "the event is validated" {
        val f = Fixture(complete, okPicker)
        shouldThrow<ValidationException> { f.create.execute(UUID.randomUUID(), "  ") }
        shouldThrow<ValidationException> { f.create.execute(UUID.randomUUID(), "x".repeat(201)) }
    }

    "processing walks QUEUED → PICKING → RENDERING → READY and confirms the debit" {
        val f = Fixture(complete, okPicker)
        val user = UUID.randomUUID()
        val look = f.create.execute(user, "brunch")

        f.process.execute(look.id)

        val ready = f.looks.findById(look.id)!!
        ready.status shouldBe LookStatus.READY
        ready.clotheIds shouldContainExactly listOf(top.id, bottom.id, shoes.id)
        ready.stylistNote shouldBe "Fresco y elegante."
        ready.imageUrl!!.startsWith("/api/v1/images/") shouldBe true
        f.wallet.confirmed shouldContainExactly listOf(look.id)
        f.wallet.refunded shouldBe emptyList()
        f.locales shouldContainExactly listOf(AppLocale.ES)
        f.aiOperations shouldContainExactly listOf("PICK", "RENDER")
        f.savedImages shouldContainExactly listOf(user)
    }

    "an AI failure while picking marks the look FAILED and refunds the credit" {
        val f = Fixture(complete, { _, _, _ -> error("gemini 503") })
        val look = f.create.execute(UUID.randomUUID(), "brunch")

        f.process.execute(look.id)

        val failed = f.looks.findById(look.id)!!
        failed.status shouldBe LookStatus.FAILED
        failed.failureCode shouldBe LookFailureCodes.AI_UNAVAILABLE
        f.wallet.refunded shouldContainExactly listOf(look.id)
        f.wallet.credits shouldBe 1
    }

    "a render failure keeps the pick, fails the look and refunds" {
        val f = Fixture(complete, okPicker, renderer = { _, _ -> error("no image") })
        val look = f.create.execute(UUID.randomUUID(), "brunch")

        f.process.execute(look.id)

        val failed = f.looks.findById(look.id)!!
        failed.status shouldBe LookStatus.FAILED
        failed.stylistNote shouldBe "Fresco y elegante."
        f.wallet.refunded shouldContainExactly listOf(look.id)
    }

    "a wardrobe emptied after queuing fails with WARDROBE_INCOMPLETE and refunds" {
        val f = Fixture(complete, okPicker)
        val look = f.create.execute(UUID.randomUUID(), "brunch")
        f.snapshot = WardrobeSnapshot(emptyList(), null)

        f.process.execute(look.id)

        f.looks.findById(look.id)!!.failureCode shouldBe LookFailureCodes.WARDROBE_INCOMPLETE
        f.wallet.refunded shouldContainExactly listOf(look.id)
    }

    "a look deleted while processing is abandoned and its credit refunded once" {
        val f = Fixture(complete, okPicker)
        val user = UUID.randomUUID()
        val look = f.create.execute(user, "brunch")
        DeleteLookService(f.looks, f.wallet).execute(user, look.id)

        f.process.execute(look.id)

        f.wallet.refunded shouldContainExactly listOf(look.id)
        f.wallet.confirmed shouldBe emptyList()
    }

    "stuck looks older than 10 minutes are failed with INTERRUPTED and refunded" {
        val f = Fixture(complete, okPicker, credits = 2)
        val stale = f.create.execute(UUID.randomUUID(), "old")
        f.clock.advance(Duration.ofMinutes(11))
        val fresh = f.create.execute(UUID.randomUUID(), "new")

        f.recover.execute() shouldBe 1

        f.looks.findById(stale.id)!!.failureCode shouldBe LookFailureCodes.INTERRUPTED
        f.looks.findById(fresh.id)!!.status shouldBe LookStatus.QUEUED
        f.wallet.refunded shouldContainExactly listOf(stale.id)
    }

    "saving, listing and deleting looks is scoped to the owner" {
        val f = Fixture(complete, okPicker, credits = 2)
        val user = UUID.randomUUID()
        val ready = f.create.execute(user, "brunch").also { f.process.execute(it.id) }
        val queued = f.create.execute(user, "cena")

        shouldThrow<ConflictException> { SaveLookService(f.looks).execute(user, queued.id) }
        shouldThrow<NotFoundException> { SaveLookService(f.looks).execute(UUID.randomUUID(), ready.id) }
        SaveLookService(f.looks).execute(user, ready.id).saved shouldBe true

        ListLooksService(f.looks, f.policy).execute(user, savedOnly = true).map { it.id } shouldContainExactly listOf(ready.id)
        ListLooksService(f.looks, f.policy).execute(user, savedOnly = false).size shouldBe 2

        DeleteLookService(f.looks, f.wallet).execute(user, ready.id)
        f.wallet.refunded shouldBe emptyList()
        shouldThrow<NotFoundException> { GetLookService(f.looks).execute(user, ready.id) }
    }
})
