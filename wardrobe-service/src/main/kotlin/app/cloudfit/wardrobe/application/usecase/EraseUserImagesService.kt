package app.cloudfit.wardrobe.application.usecase

import app.cloudfit.shared.application.port.ImageStore
import app.cloudfit.wardrobe.application.port.input.EraseUserImagesUseCase
import app.cloudfit.wardrobe.application.port.output.UserImageRepository
import java.util.UUID
import org.slf4j.LoggerFactory

class EraseUserImagesService(
    private val store: ImageStore,
    private val images: UserImageRepository,
) : EraseUserImagesUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun execute(userId: UUID) {
        images.listKeys(userId).forEach { key ->
            runCatching { store.delete(key) }.onFailure { log.warn("Could not delete image {}: {}", key, it.message) }
        }
    }
}
