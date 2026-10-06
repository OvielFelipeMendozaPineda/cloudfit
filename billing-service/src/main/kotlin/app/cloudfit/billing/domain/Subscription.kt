package app.cloudfit.billing.domain

import java.time.Instant
import java.util.UUID

enum class SubscriptionStatus {
    ACTIVE,
    PAST_DUE,
    CANCELED,
    INCOMPLETE,
    ;

    companion object {
        fun fromStripe(status: String): SubscriptionStatus = when (status.lowercase()) {
            "active", "trialing" -> ACTIVE
            "past_due", "unpaid" -> PAST_DUE
            "canceled", "incomplete_expired" -> CANCELED
            else -> INCOMPLETE
        }
    }
}

data class Subscription(
    val id: UUID,
    val userId: UUID,
    val provider: PaymentProvider,
    val providerSubscriptionId: String?,
    val planCode: String,
    val status: SubscriptionStatus,
    val currentPeriodEnd: Instant?,
    val cancelAtPeriodEnd: Boolean,
) {
    fun isActive(now: Instant): Boolean =
        status == SubscriptionStatus.ACTIVE && (currentPeriodEnd == null || currentPeriodEnd.isAfter(now))
}

data class ActivePlan(
    val code: String,
    val status: SubscriptionStatus,
    val renewsAt: Instant?,
)

data class WalletSummary(
    val balance: CreditBalance,
    val plan: ActivePlan?,
)
