package app.cloudfit.accounts.domain

data class ExternalIdentity(
    val provider: AuthProvider,
    val subject: String,
    val email: String?,
    val emailVerified: Boolean,
    val displayName: String?,
    val locale: String?,
)
