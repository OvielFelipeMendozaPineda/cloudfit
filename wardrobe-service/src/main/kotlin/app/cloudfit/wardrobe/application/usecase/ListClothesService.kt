package app.cloudfit.wardrobe.application.usecase

import app.cloudfit.wardrobe.application.port.input.ListClothesUseCase
import app.cloudfit.wardrobe.application.port.output.ClothesRepository
import app.cloudfit.wardrobe.domain.Clothe
import java.util.UUID

class ListClothesService(private val clothes: ClothesRepository) : ListClothesUseCase {
    override suspend fun execute(userId: UUID): List<Clothe> = clothes.listByUser(userId)
}
