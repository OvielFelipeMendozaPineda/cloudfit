package app.cloudfit.accounts.domain

import app.cloudfit.shared.domain.AppLocale
import java.time.Instant
import java.util.UUID

data class CreditsView(
    val balance: Int,
    val plan: Int,
    val free: Int,
    val pack: Int,
    val reward: Int,
)

data class PlanView(
    val code: String,
    val status: String,
    val renewsAt: Instant?,
)

data class AccountBalance(
    val credits: CreditsView,
    val plan: PlanView?,
)

data class Me(
    val id: UUID,
    val email: String,
    val emailVerified: Boolean,
    val displayName: String?,
    val locale: AppLocale,
    val avatarUrl: String?,
    val providers: List<AuthProvider>,
    val credits: CreditsView,
    val plan: PlanView?,
    val adsEnabled: Boolean,
    val maxClothes: Int?,
)

data class Session(
    val accessToken: AccessToken,
    val refreshToken: String,
    val refreshExpiresAt: Instant,
    val me: Me,
)

data class AuthProviders(
    val googleClientId: String?,
    val appleClientId: String?,
    val appleRedirectUri: String?,
)

data class AccountProfile(
    val email: String,
    val locale: AppLocale,
)
