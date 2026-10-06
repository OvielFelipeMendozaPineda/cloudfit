package app.cloudfit.styling.application.usecase

import app.cloudfit.shared.application.error.ErrorCodes
import app.cloudfit.shared.application.error.UnprocessableException
import app.cloudfit.shared.application.error.ValidationException
import app.cloudfit.shared.application.port.ClockProvider
import app.cloudfit.shared.application.port.TransactionRunner
import app.cloudfit.styling.application.port.input.CreateLookUseCase
import app.cloudfit.styling.application.port.output.CreditWallet
import app.cloudfit.styling.application.port.output.LookJobScheduler
import app.cloudfit.styling.application.port.output.LookRepository
import app.cloudfit.styling.application.port.output.WardrobeReader
import app.cloudfit.styling.domain.Look
import app.cloudfit.styling.domain.LookStatus
import app.cloudfit.styling.domain.StylingPolicy
import java.util.UUID

class CreateLookService(
    private val looks: LookRepository,
    private val wardrobe: WardrobeReader,
    private val wallet: CreditWallet,
    private val scheduler: LookJobScheduler,
    private val policy: StylingPolicy,
    private val tx: TransactionRunner,
    private val clock: ClockProvider,
) : CreateLookUseCase {
    override suspend fun execute(userId: UUID, event: String): Look {
        val trimmed = event.trim()
        if (trimmed.isEmpty() || trimmed.length > policy.maxEventLength) {
            throw ValidationException("event is required (max ${policy.maxEventLength} chars)")
        }
        if (!wardrobe.snapshot(userId).isComplete) {
            throw UnprocessableException("Wardrobe needs (top + bottom or dress) and shoes", ErrorCodes.WARDROBE_INCOMPLETE)
        }
        val look = Look(
            id = UUID.randomUUID(),
            userId = userId,
            event = trimmed,
            status = LookStatus.QUEUED,
            clotheIds = emptyList(),
            stylistNote = null,
            imageUrl = null,
            saved = false,
            failureCode = null,
            createdAt = clock.now(),
        )
        tx.inTransaction {
            wallet.reserve(userId, look.id)
            looks.create(look)
        }
        scheduler.schedule(look.id)
        return look
    }
}
