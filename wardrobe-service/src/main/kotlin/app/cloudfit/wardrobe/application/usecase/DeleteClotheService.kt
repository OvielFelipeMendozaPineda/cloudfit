package app.cloudfit.wardrobe.application.usecase

import app.cloudfit.shared.application.error.NotFoundException
import app.cloudfit.wardrobe.application.port.input.DeleteClotheUseCase
import app.cloudfit.wardrobe.application.port.output.ClothesRepository
import java.util.UUID

class DeleteClotheService(private val clothes: ClothesRepository) : DeleteClotheUseCase {
    override suspend fun execute(userId: UUID, id: UUID) {
        if (!clothes.delete(userId, id)) throw NotFoundException("Clothe $id not found")
    }
}
