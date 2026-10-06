package app.cloudfit.wardrobe.application.port.input

import app.cloudfit.shared.application.port.ImageContent
import app.cloudfit.wardrobe.domain.ClotheTags
import java.util.UUID

interface UploadImageUseCase {
    suspend fun execute(userId: UUID, bytes: ByteArray, contentType: String): String
}

interface RemoveBackgroundUseCase {
    suspend fun execute(userId: UUID, bytes: ByteArray, contentType: String): String
}

interface TagClotheUseCase {
    suspend fun execute(userId: UUID, bytes: ByteArray, contentType: String): ClotheTags
}

interface GetImageUseCase {
    suspend fun execute(key: String): ImageContent?
}

interface EraseUserImagesUseCase {
    suspend fun execute(userId: UUID)
}
