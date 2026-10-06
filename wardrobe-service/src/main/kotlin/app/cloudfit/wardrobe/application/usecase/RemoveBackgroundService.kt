package app.cloudfit.wardrobe.application.usecase

import app.cloudfit.shared.application.error.ProviderNotConfiguredException
import app.cloudfit.shared.application.error.UpstreamException
import app.cloudfit.wardrobe.application.port.input.RemoveBackgroundUseCase
import app.cloudfit.wardrobe.application.port.input.UploadImageUseCase
import app.cloudfit.wardrobe.application.port.output.BackgroundRemover
import java.util.UUID
import org.slf4j.LoggerFactory

class RemoveBackgroundService(
    private val remover: BackgroundRemover?,
    private val upload: UploadImageUseCase,
) : RemoveBackgroundUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun execute(userId: UUID, bytes: ByteArray, contentType: String): String {
        val active = remover ?: throw ProviderNotConfiguredException("REMOVE_BG_API_KEY is not set")
        val png = try {
            active.remove(bytes, contentType)
        } catch (e: Exception) {
            log.warn("remove.bg failed: {}", e.message)
            throw UpstreamException("Background removal failed")
        }
        return upload.execute(userId, png, "image/png")
    }
}
