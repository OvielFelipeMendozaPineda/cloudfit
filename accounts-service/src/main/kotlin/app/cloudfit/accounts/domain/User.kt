package app.cloudfit.accounts.domain

import app.cloudfit.shared.domain.AppLocale
import java.time.Instant
import java.util.UUID

data class User(
    val id: UUID,
    val email: String,
    val passwordHash: String?,
    val emailVerifiedAt: Instant?,
    val displayName: String?,
    val locale: AppLocale,
    val createdAt: Instant,
) {
    val emailVerified: Boolean get() = emailVerifiedAt != null
}
