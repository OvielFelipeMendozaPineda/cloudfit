package app.cloudfit.wardrobe.application.port.output

import java.util.UUID

interface AvatarRepository {
    suspend fun find(userId: UUID): String?

    suspend fun upsert(userId: UUID, photoUrl: String)

    suspend fun delete(userId: UUID): Boolean
}
