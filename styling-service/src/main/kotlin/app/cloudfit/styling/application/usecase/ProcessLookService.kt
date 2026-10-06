package app.cloudfit.styling.application.usecase

import app.cloudfit.shared.application.ai.AiCallContext
import app.cloudfit.styling.application.port.input.ProcessLookUseCase
import app.cloudfit.styling.application.port.output.ClothePicker
import app.cloudfit.styling.application.port.output.CreditWallet
import app.cloudfit.styling.application.port.output.LookRepository
import app.cloudfit.styling.application.port.output.OutfitImageRenderer
import app.cloudfit.styling.application.port.output.RenderedImageSaver
import app.cloudfit.styling.application.port.output.UserLocaleReader
import app.cloudfit.styling.application.port.output.WardrobeReader
import app.cloudfit.styling.domain.Look
import app.cloudfit.styling.domain.LookFailureCodes
import app.cloudfit.styling.domain.LookStatus
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

class ProcessLookService(
    private val looks: LookRepository,
    private val wardrobe: WardrobeReader,
    private val locales: UserLocaleReader,
    private val picker: ClothePicker,
    private val renderer: OutfitImageRenderer,
    private val images: RenderedImageSaver,
    private val wallet: CreditWallet,
) : ProcessLookUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun execute(lookId: UUID) {
        val queued = looks.findById(lookId) ?: return
        if (queued.status != LookStatus.QUEUED) return
        var current = queued
        try {
            current = advance(current.copy(status = LookStatus.PICKING))
            val snapshot = wardrobe.snapshot(current.userId)
            if (!snapshot.isComplete) throw LookFailure(LookFailureCodes.WARDROBE_INCOMPLETE, "wardrobe incomplete")
            val locale = locales.locale(current.userId)
            val pick = ai(current.userId, "PICK") { picker.pick(current.event, snapshot.items, locale) }
            current = advance(current.copy(status = LookStatus.RENDERING, clotheIds = pick.clotheIds, stylistNote = pick.stylistNote))
            val chosen = snapshot.items.filter { it.id in pick.clotheIds.toSet() }
            val image = ai(current.userId, "RENDER") { renderer.render(chosen, snapshot.avatarUrl) }
            val imageUrl = images.save(current.userId, image.bytes, image.mimeType)
            current = advance(current.copy(status = LookStatus.READY, imageUrl = imageUrl))
            wallet.confirm(current.id)
        } catch (e: LookAbandoned) {
            log.info("Look {} abandoned: {}", lookId, e.message)
        } catch (e: CancellationException) {
            throw e
        } catch (e: LookFailure) {
            fail(current, e.code, e.message)
        } catch (e: Exception) {
            log.error("Look {} failed unexpectedly", lookId, e)
            fail(current, LookFailureCodes.INTERNAL_ERROR, e.message)
        }
    }

    private suspend fun advance(look: Look): Look {
        if (!looks.updateProgress(look)) throw LookAbandoned("look is no longer in progress")
        return look
    }

    private suspend fun fail(look: Look, code: String, reason: String?) {
        log.warn("Look {} failed with {}: {}", look.id, code, reason)
        looks.updateProgress(look.copy(status = LookStatus.FAILED, failureCode = code))
        wallet.refund(look.id)
    }

    private suspend fun <T> ai(userId: UUID, operation: String, block: suspend () -> T): T =
        try {
            withContext(AiCallContext(userId, operation)) { block() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw LookFailure(LookFailureCodes.AI_UNAVAILABLE, "$operation failed: ${e.message}")
        }

    private class LookFailure(val code: String, message: String) : RuntimeException(message)

    private class LookAbandoned(message: String) : RuntimeException(message)
}
