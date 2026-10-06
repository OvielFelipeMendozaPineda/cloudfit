package app.cloudfit.wardrobe.application.usecase

import app.cloudfit.shared.application.port.ImageStore
import app.cloudfit.wardrobe.application.port.input.UploadImageUseCase
import app.cloudfit.wardrobe.application.port.output.UserImageRepository
import java.util.UUID

class UploadImageService(
    private val store: ImageStore,
    private val images: UserImageRepository,
) : UploadImageUseCase {
    override suspend fun execute(userId: UUID, bytes: ByteArray, contentType: String): String {
        val stored = store.put(bytes, contentType)
        images.record(userId, stored)
        return stored.url
    }
}
