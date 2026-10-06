package app.cloudfit.accounts.domain

import java.time.Duration

data class AccountsPolicy(
    val appUrl: String,
    val refreshTokenTtl: Duration = Duration.ofDays(30),
    val verifyTokenTtl: Duration = Duration.ofHours(24),
    val resetTokenTtl: Duration = Duration.ofHours(1),
    val appleRedirectUri: String? = null,
)
