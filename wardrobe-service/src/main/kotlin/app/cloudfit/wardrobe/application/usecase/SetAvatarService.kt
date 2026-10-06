package app.cloudfit.wardrobe.application.usecase

import app.cloudfit.wardrobe.application.port.input.SetAvatarUseCase
import app.cloudfit.wardrobe.application.port.output.AvatarRepository
import java.util.UUID

class SetAvatarService(private val avatars: AvatarRepository) : SetAvatarUseCase {
    override suspend fun execute(userId: UUID, photoUrl: String): String {
        val url = WardrobeValidation.url(photoUrl, "photoUrl")
        avatars.upsert(userId, url)
        return url
    }
}
