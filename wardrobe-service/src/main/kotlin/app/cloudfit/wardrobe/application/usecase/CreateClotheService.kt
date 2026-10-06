package app.cloudfit.wardrobe.application.usecase

import app.cloudfit.shared.application.error.ErrorCodes
import app.cloudfit.shared.application.error.ForbiddenException
import app.cloudfit.wardrobe.application.port.input.CreateClotheUseCase
import app.cloudfit.wardrobe.application.port.input.GetWardrobeLimitsUseCase
import app.cloudfit.wardrobe.application.port.output.ClothesRepository
import app.cloudfit.wardrobe.domain.Clothe
import app.cloudfit.wardrobe.domain.ClotheDraft
import java.util.UUID

class CreateClotheService(
    private val clothes: ClothesRepository,
    private val limits: GetWardrobeLimitsUseCase,
) : CreateClotheUseCase {
    override suspend fun execute(userId: UUID, draft: ClotheDraft): Clothe {
        val valid = WardrobeValidation.draft(draft)
        val max = limits.execute(userId)
        if (max != null && clothes.countByUser(userId) >= max) {
            throw ForbiddenException("Wardrobe limit of $max clothes reached", ErrorCodes.WARDROBE_LIMIT_REACHED)
        }
        val clothe = valid.toClothe(UUID.randomUUID(), userId)
        clothes.insert(clothe)
        return clothe
    }
}

internal fun ClotheDraft.toClothe(id: UUID, userId: UUID) = Clothe(
    id = id,
    userId = userId,
    category = category,
    imageUrl = imageUrl,
    name = name,
    colour = colour,
    pattern = pattern,
    formality = formality,
    warmth = warmth,
    description = description,
)
