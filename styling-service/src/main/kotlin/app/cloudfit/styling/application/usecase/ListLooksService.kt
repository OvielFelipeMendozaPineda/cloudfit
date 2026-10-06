package app.cloudfit.styling.application.usecase

import app.cloudfit.styling.application.port.input.ListLooksUseCase
import app.cloudfit.styling.application.port.output.LookRepository
import app.cloudfit.styling.domain.Look
import app.cloudfit.styling.domain.StylingPolicy
import java.util.UUID

class ListLooksService(
    private val looks: LookRepository,
    private val policy: StylingPolicy,
) : ListLooksUseCase {
    override suspend fun execute(userId: UUID, savedOnly: Boolean): List<Look> =
        looks.list(userId, savedOnly, policy.listLimit)
}
