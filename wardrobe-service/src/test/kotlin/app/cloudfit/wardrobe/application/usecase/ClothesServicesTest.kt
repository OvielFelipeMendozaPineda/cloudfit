package app.cloudfit.wardrobe.application.usecase

import app.cloudfit.shared.application.error.ErrorCodes
import app.cloudfit.shared.application.error.ForbiddenException
import app.cloudfit.shared.application.error.NotFoundException
import app.cloudfit.shared.application.error.ValidationException
import app.cloudfit.shared.domain.ClothingCategory
import app.cloudfit.wardrobe.domain.ClotheDraft
import app.cloudfit.wardrobe.domain.WardrobePolicy
import app.cloudfit.wardrobe.support.InMemoryClothesRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.util.UUID

class ClothesServicesTest : StringSpec({
    val draft = ClotheDraft(category = ClothingCategory.TOP, imageUrl = "/api/v1/images/x.png", name = " White tee ")

    fun services(hasPlan: Boolean = false, max: Int = 3): Pair<InMemoryClothesRepository, CreateClotheService> {
        val repo = InMemoryClothesRepository()
        val limits = GetWardrobeLimitsService({ hasPlan }, WardrobePolicy(freeMaxClothes = max))
        return repo to CreateClotheService(repo, limits)
    }

    "free users hit WARDROBE_LIMIT_REACHED at the configured maximum" {
        val (_, create) = services(max = 2)
        val user = UUID.randomUUID()
        create.execute(user, draft)
        create.execute(user, draft)

        shouldThrow<ForbiddenException> { create.execute(user, draft) }.code shouldBe ErrorCodes.WARDROBE_LIMIT_REACHED
        create.execute(UUID.randomUUID(), draft).name shouldBe "White tee"
    }

    "users with an active plan have no wardrobe limit" {
        val (repo, create) = services(hasPlan = true, max = 1)
        val user = UUID.randomUUID()
        repeat(5) { create.execute(user, draft) }
        repo.countByUser(user) shouldBe 5
        GetWardrobeLimitsService({ true }, WardrobePolicy()).execute(user) shouldBe null
        GetWardrobeLimitsService({ false }, WardrobePolicy()).execute(user) shouldBe 30
    }

    "clothes are scoped to their owner" {
        val (repo, create) = services()
        val owner = UUID.randomUUID()
        val intruder = UUID.randomUUID()
        val clothe = create.execute(owner, draft)

        ListClothesService(repo).execute(intruder) shouldHaveSize 0
        shouldThrow<NotFoundException> { UpdateClotheService(repo).execute(intruder, clothe.id, draft) }
        shouldThrow<NotFoundException> { DeleteClotheService(repo).execute(intruder, clothe.id) }

        UpdateClotheService(repo).execute(owner, clothe.id, draft.copy(colour = "white")).colour shouldBe "white"
        DeleteClotheService(repo).execute(owner, clothe.id)
        ListClothesService(repo).execute(owner) shouldHaveSize 0
    }

    "drafts are validated" {
        val (_, create) = services()
        shouldThrow<ValidationException> { create.execute(UUID.randomUUID(), draft.copy(imageUrl = " ")) }
        shouldThrow<ValidationException> { create.execute(UUID.randomUUID(), draft.copy(description = "x".repeat(501))) }
    }
})
