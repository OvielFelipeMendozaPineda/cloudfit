package app.cloudfit.accounts.infrastructure.adapter.input.http.dto

import app.cloudfit.accounts.domain.Me
import app.cloudfit.accounts.domain.Session
import kotlinx.serialization.Serializable

@Serializable
data class CreditsDto(
    val balance: Int,
    val plan: Int,
    val free: Int,
    val pack: Int,
    val reward: Int,
)

@Serializable
data class PlanDto(
    val code: String,
    val status: String,
    val renewsAt: String?,
)

@Serializable
data class LimitsDto(val maxClothes: Int?)

@Serializable
data class MeDto(
    val id: String,
    val email: String,
    val emailVerified: Boolean,
    val displayName: String?,
    val locale: String,
    val avatarUrl: String?,
    val providers: List<String>,
    val credits: CreditsDto,
    val plan: PlanDto?,
    val adsEnabled: Boolean,
    val limits: LimitsDto,
) {
    companion object {
        fun from(me: Me) = MeDto(
            id = me.id.toString(),
            email = me.email,
            emailVerified = me.emailVerified,
            displayName = me.displayName,
            locale = me.locale.code,
            avatarUrl = me.avatarUrl,
            providers = me.providers.map { it.name },
            credits = CreditsDto(me.credits.balance, me.credits.plan, me.credits.free, me.credits.pack, me.credits.reward),
            plan = me.plan?.let { PlanDto(it.code, it.status, it.renewsAt?.toString()) },
            adsEnabled = me.adsEnabled,
            limits = LimitsDto(me.maxClothes),
        )
    }
}

@Serializable
data class UpdateMeRequestDto(
    val displayName: String? = null,
    val locale: String? = null,
)

fun Session.toDto() = SessionDto(
    accessToken = accessToken.token,
    expiresIn = accessToken.expiresInSeconds,
    user = MeDto.from(me),
)
