package app.cloudfit.wardrobe.application.usecase

import app.cloudfit.shared.application.error.NotFoundException
import app.cloudfit.wardrobe.application.port.input.UpdateClotheUseCase
import app.cloudfit.wardrobe.application.port.output.ClothesRepository
import app.cloudfit.wardrobe.domain.Clothe
import app.cloudfit.wardrobe.domain.ClotheDraft
import java.util.UUID

class UpdateClotheService(private val clothes: ClothesRepository) : UpdateClotheUseCase {
    override suspend fun execute(userId: UUID, id: UUID, draft: ClotheDraft): Clothe {
        val valid = WardrobeValidation.draft(draft)
        clothes.find(userId, id) ?: throw NotFoundException("Clothe $id not found")
        val updated = valid.toClothe(id, userId)
        clothes.update(updated)
        return updated
    }
}
