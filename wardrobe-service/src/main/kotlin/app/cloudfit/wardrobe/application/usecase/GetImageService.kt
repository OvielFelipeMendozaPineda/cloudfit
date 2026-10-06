package app.cloudfit.wardrobe.application.usecase

import app.cloudfit.shared.application.port.ImageContent
import app.cloudfit.shared.application.port.ImageStore
import app.cloudfit.wardrobe.application.port.input.GetImageUseCase

class GetImageService(private val store: ImageStore) : GetImageUseCase {
    override suspend fun execute(key: String): ImageContent? = store.readByKey(key)
}
