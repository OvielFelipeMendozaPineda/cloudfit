package app.cloudfit.wardrobe.application.usecase

import app.cloudfit.shared.application.ai.AiCallContext
import app.cloudfit.shared.application.error.ErrorCodes
import app.cloudfit.shared.application.error.UpstreamException
import app.cloudfit.wardrobe.application.port.input.TagClotheUseCase
import app.cloudfit.wardrobe.application.port.output.ClotheTagger
import app.cloudfit.wardrobe.domain.ClotheTags
import java.util.UUID
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

class TagClotheService(private val tagger: ClotheTagger) : TagClotheUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun execute(userId: UUID, bytes: ByteArray, contentType: String): ClotheTags =
        try {
            withContext(AiCallContext(userId, OPERATION)) { tagger.tag(bytes, contentType) }
        } catch (e: Exception) {
            log.warn("Tagging failed: {}", e.message)
            throw UpstreamException("Could not tag the garment", ErrorCodes.AI_UNAVAILABLE)
        }

    private companion object {
        const val OPERATION = "TAG"
    }
}
