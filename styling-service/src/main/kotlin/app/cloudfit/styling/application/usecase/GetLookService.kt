package app.cloudfit.styling.application.usecase

import app.cloudfit.shared.application.error.NotFoundException
import app.cloudfit.styling.application.port.input.GetLookUseCase
import app.cloudfit.styling.application.port.output.LookRepository
import app.cloudfit.styling.domain.Look
import java.util.UUID

class GetLookService(private val looks: LookRepository) : GetLookUseCase {
    override suspend fun execute(userId: UUID, id: UUID): Look =
        looks.find(userId, id) ?: throw NotFoundException("Look $id not found")
}
