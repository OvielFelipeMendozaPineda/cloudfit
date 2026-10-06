package app.cloudfit.accounts.infrastructure.adapter.input.http.dto

import app.cloudfit.accounts.domain.AuthProviders
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequestDto(
    val email: String = "",
    val password: String = "",
    val locale: String? = null,
    val displayName: String? = null,
)

@Serializable
data class VerifyEmailRequestDto(val token: String = "")

@Serializable
data class EmailRequestDto(val email: String = "")

@Serializable
data class LoginRequestDto(
    val email: String = "",
    val password: String = "",
)

@Serializable
data class SocialLoginRequestDto(
    val idToken: String = "",
    val displayName: String? = null,
)

@Serializable
data class ResetPasswordRequestDto(
    val token: String = "",
    val password: String = "",
)

@Serializable
data class StatusResponseDto(val status: String)

@Serializable
data class SessionDto(
    val accessToken: String,
    val expiresIn: Long,
    val user: MeDto,
)

@Serializable
data class GoogleProviderDto(val clientId: String)

@Serializable
data class AppleProviderDto(
    val clientId: String,
    val redirectUri: String,
)

@Serializable
data class AuthProvidersDto(
    val google: GoogleProviderDto?,
    val apple: AppleProviderDto?,
) {
    companion object {
        fun from(providers: AuthProviders) = AuthProvidersDto(
            google = providers.googleClientId?.let { GoogleProviderDto(it) },
            apple = providers.appleClientId?.let { AppleProviderDto(it, providers.appleRedirectUri.orEmpty()) },
        )
    }
}
