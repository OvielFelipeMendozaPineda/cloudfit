package app.cloudfit.wardrobe.application.usecase

import app.cloudfit.wardrobe.application.port.input.GetWardrobeLimitsUseCase
import app.cloudfit.wardrobe.application.port.output.PlanStatusReader
import app.cloudfit.wardrobe.domain.WardrobePolicy
import java.util.UUID

class GetWardrobeLimitsService(
    private val plans: PlanStatusReader,
    private val policy: WardrobePolicy,
) : GetWardrobeLimitsUseCase {
    override suspend fun execute(userId: UUID): Int? = if (plans.hasActivePlan(userId)) null else policy.freeMaxClothes
}
