package app.cloudfit.accounts.infrastructure.adapter.input.http

import app.cloudfit.accounts.application.port.input.ForgotPasswordUseCase
import app.cloudfit.accounts.application.port.input.GetAuthProvidersUseCase
import app.cloudfit.accounts.application.port.input.LoginUseCase
import app.cloudfit.accounts.application.port.input.LogoutUseCase
import app.cloudfit.accounts.application.port.input.RefreshSessionUseCase
import app.cloudfit.accounts.application.port.input.RegisterCommand
import app.cloudfit.accounts.application.port.input.RegisterUseCase
import app.cloudfit.accounts.application.port.input.ResendVerificationUseCase
import app.cloudfit.accounts.application.port.input.ResetPasswordUseCase
import app.cloudfit.accounts.application.port.input.SocialLoginUseCase
import app.cloudfit.accounts.application.port.input.VerifyEmailUseCase
import app.cloudfit.accounts.domain.AuthProvider
import app.cloudfit.accounts.domain.Session
import app.cloudfit.accounts.infrastructure.adapter.input.http.dto.AuthProvidersDto
import app.cloudfit.accounts.infrastructure.adapter.input.http.dto.EmailRequestDto
import app.cloudfit.accounts.infrastructure.adapter.input.http.dto.LoginRequestDto
import app.cloudfit.accounts.infrastructure.adapter.input.http.dto.RegisterRequestDto
import app.cloudfit.accounts.infrastructure.adapter.input.http.dto.ResetPasswordRequestDto
import app.cloudfit.accounts.infrastructure.adapter.input.http.dto.SocialLoginRequestDto
import app.cloudfit.accounts.infrastructure.adapter.input.http.dto.StatusResponseDto
import app.cloudfit.accounts.infrastructure.adapter.input.http.dto.VerifyEmailRequestDto
import app.cloudfit.accounts.infrastructure.adapter.input.http.dto.toDto
import app.cloudfit.shared.application.error.AppException
import app.cloudfit.shared.application.error.ForbiddenException
import app.cloudfit.shared.infrastructure.http.AUTH_RATE_LIMIT
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

data class AuthUseCases(
    val providers: GetAuthProvidersUseCase,
    val register: RegisterUseCase,
    val verifyEmail: VerifyEmailUseCase,
    val resendVerification: ResendVerificationUseCase,
    val login: LoginUseCase,
    val socialLogin: SocialLoginUseCase,
    val refresh: RefreshSessionUseCase,
    val logout: LogoutUseCase,
    val forgotPassword: ForgotPasswordUseCase,
    val resetPassword: ResetPasswordUseCase,
)

const val CLIENT_HEADER = "X-CloudFit-Client"

fun Route.registerAuthRoutes(useCases: AuthUseCases, cookie: RefreshCookieSettings) {
    rateLimit(AUTH_RATE_LIMIT) {
        route("/auth") {
            get("/providers") {
                call.respond(AuthProvidersDto.from(useCases.providers.execute()))
            }
            post("/register") {
                val body = call.receive<RegisterRequestDto>()
                useCases.register.execute(RegisterCommand(body.email, body.password, body.locale, body.displayName))
                call.respond(HttpStatusCode.Accepted, StatusResponseDto("VERIFY_EMAIL"))
            }
            post("/verify-email") {
                val body = call.receive<VerifyEmailRequestDto>()
                call.respondSession(useCases.verifyEmail.execute(body.token), cookie)
            }
            post("/resend-verification") {
                useCases.resendVerification.execute(call.receive<EmailRequestDto>().email)
                call.respond(HttpStatusCode.Accepted, StatusResponseDto("VERIFY_EMAIL"))
            }
            post("/login") {
                val body = call.receive<LoginRequestDto>()
                call.respondSession(useCases.login.execute(body.email, body.password), cookie)
            }
            post("/google") {
                val body = call.receive<SocialLoginRequestDto>()
                call.respondSession(useCases.socialLogin.execute(AuthProvider.GOOGLE, body.idToken, body.displayName), cookie)
            }
            post("/apple") {
                val body = call.receive<SocialLoginRequestDto>()
                call.respondSession(useCases.socialLogin.execute(AuthProvider.APPLE, body.idToken, body.displayName), cookie)
            }
            post("/refresh") {
                call.requireWebClient()
                val session = try {
                    useCases.refresh.execute(call.readRefreshCookie(cookie))
                } catch (e: AppException) {
                    call.clearRefreshCookie(cookie)
                    throw e
                }
                call.respondSession(session, cookie)
            }
            post("/logout") {
                call.requireWebClient()
                useCases.logout.execute(call.readRefreshCookie(cookie))
                call.clearRefreshCookie(cookie)
                call.respond(HttpStatusCode.NoContent)
            }
            post("/forgot-password") {
                useCases.forgotPassword.execute(call.receive<EmailRequestDto>().email)
                call.respond(HttpStatusCode.Accepted, StatusResponseDto("CHECK_EMAIL"))
            }
            post("/reset-password") {
                val body = call.receive<ResetPasswordRequestDto>()
                useCases.resetPassword.execute(body.token, body.password)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private suspend fun ApplicationCall.respondSession(session: Session, cookie: RefreshCookieSettings) {
    writeRefreshCookie(cookie, session)
    respond(session.toDto())
}

private fun ApplicationCall.requireWebClient() {
    if (request.headers[CLIENT_HEADER] != "web") throw ForbiddenException("Missing $CLIENT_HEADER: web header")
}
