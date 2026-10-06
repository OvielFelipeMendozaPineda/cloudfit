package app.cloudfit.wardrobe.application.port.input

import java.util.UUID

interface GetAvatarUseCase {
    suspend fun execute(userId: UUID): String?
}

interface SetAvatarUseCase {
    suspend fun execute(userId: UUID, photoUrl: String): String
}

interface DeleteAvatarUseCase {
    suspend fun execute(userId: UUID)
}
