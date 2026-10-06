package app.cloudfit.wardrobe.application.usecase

import app.cloudfit.wardrobe.application.port.input.DeleteAvatarUseCase
import app.cloudfit.wardrobe.application.port.output.AvatarRepository
import java.util.UUID

class DeleteAvatarService(private val avatars: AvatarRepository) : DeleteAvatarUseCase {
    override suspend fun execute(userId: UUID) {
        avatars.delete(userId)
    }
}
