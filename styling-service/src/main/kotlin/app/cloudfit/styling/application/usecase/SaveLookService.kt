package app.cloudfit.styling.application.usecase

import app.cloudfit.shared.application.error.ConflictException
import app.cloudfit.shared.application.error.NotFoundException
import app.cloudfit.styling.application.port.input.SaveLookUseCase
import app.cloudfit.styling.application.port.output.LookRepository
import app.cloudfit.styling.domain.Look
import app.cloudfit.styling.domain.LookStatus
import java.util.UUID

class SaveLookService(private val looks: LookRepository) : SaveLookUseCase {
    override suspend fun execute(userId: UUID, id: UUID): Look {
        val look = looks.find(userId, id) ?: throw NotFoundException("Look $id not found")
        if (look.status != LookStatus.READY) throw ConflictException("Only READY looks can be saved")
        if (!look.saved) looks.markSaved(userId, id)
        return look.copy(saved = true)
    }
}
