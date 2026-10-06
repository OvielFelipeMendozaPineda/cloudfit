package app.cloudfit.wardrobe.application.port.input

import app.cloudfit.wardrobe.domain.Clothe
import app.cloudfit.wardrobe.domain.ClotheDraft
import java.util.UUID

interface ListClothesUseCase {
    suspend fun execute(userId: UUID): List<Clothe>
}

interface CreateClotheUseCase {
    suspend fun execute(userId: UUID, draft: ClotheDraft): Clothe
}

interface UpdateClotheUseCase {
    suspend fun execute(userId: UUID, id: UUID, draft: ClotheDraft): Clothe
}

interface DeleteClotheUseCase {
    suspend fun execute(userId: UUID, id: UUID)
}

interface GetWardrobeLimitsUseCase {
    suspend fun execute(userId: UUID): Int?
}
