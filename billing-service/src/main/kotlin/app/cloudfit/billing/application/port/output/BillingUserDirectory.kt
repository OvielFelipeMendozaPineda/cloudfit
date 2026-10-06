package app.cloudfit.billing.application.port.output

import java.util.UUID

fun interface BillingUserDirectory {
    suspend fun emailOf(userId: UUID): String?
}
