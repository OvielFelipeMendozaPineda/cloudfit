package app.cloudfit.wardrobe.application.port.output

import app.cloudfit.wardrobe.domain.Clothe
import java.util.UUID

interface ClothesRepository {
    suspend fun listByUser(userId: UUID): List<Clothe>

    suspend fun find(userId: UUID, id: UUID): Clothe?

    suspend fun countByUser(userId: UUID): Long

    suspend fun insert(clothe: Clothe)

    suspend fun update(clothe: Clothe)

    suspend fun delete(userId: UUID, id: UUID): Boolean
}
