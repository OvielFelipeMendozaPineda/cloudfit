package app.cloudfit.wardrobe.application.usecase

import app.cloudfit.wardrobe.application.port.input.GetAvatarUseCase
import app.cloudfit.wardrobe.application.port.output.AvatarRepository
import java.util.UUID

class GetAvatarService(private val avatars: AvatarRepository) : GetAvatarUseCase {
    override suspend fun execute(userId: UUID): String? = avatars.find(userId)
}
