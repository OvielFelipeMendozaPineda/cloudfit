package app.cloudfit.wardrobe.application.port.output

import java.util.UUID

fun interface PlanStatusReader {
    suspend fun hasActivePlan(userId: UUID): Boolean
}
