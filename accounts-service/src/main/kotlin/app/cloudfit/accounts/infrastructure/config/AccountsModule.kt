package app.cloudfit.accounts.infrastructure.config

import app.cloudfit.accounts.application.port.output.AccountBalanceReader
import app.cloudfit.accounts.application.port.output.AccountDataEraser
import app.cloudfit.accounts.application.port.output.AvatarUrlReader
import app.cloudfit.accounts.application.port.output.WardrobeLimitsReader
import app.cloudfit.accounts.application.port.output.WelcomeBonusGranter
import app.cloudfit.accounts.application.usecase.AccountMailer
import app.cloudfit.accounts.application.usecase.DeleteAccountService
import app.cloudfit.accounts.application.usecase.EmailTokenFactory
import app.cloudfit.accounts.application.usecase.ForgotPasswordService
import app.cloudfit.accounts.application.usecase.GetAccountProfileService
import app.cloudfit.accounts.application.usecase.GetAuthProvidersService
import app.cloudfit.accounts.application.usecase.GetMeService
import app.cloudfit.accounts.application.usecase.LoginService
import app.cloudfit.accounts.application.usecase.LogoutService
import app.cloudfit.accounts.application.usecase.MeAssembler
import app.cloudfit.accounts.application.usecase.RefreshSessionService
import app.cloudfit.accounts.application.usecase.RegisterService
import app.cloudfit.accounts.application.usecase.ResendVerificationService
import app.cloudfit.accounts.application.usecase.ResetPasswordService
import app.cloudfit.accounts.application.usecase.SessionIssuer
import app.cloudfit.accounts.application.usecase.SocialLoginService
import app.cloudfit.accounts.application.usecase.UpdateMeService
import app.cloudfit.accounts.application.usecase.VerifyEmailService
import app.cloudfit.accounts.infrastructure.adapter.input.http.AuthUseCases
import app.cloudfit.accounts.infrastructure.adapter.input.http.RefreshCookieSettings
import app.cloudfit.accounts.infrastructure.adapter.input.http.registerAuthRoutes
import app.cloudfit.accounts.infrastructure.adapter.input.http.registerMeRoutes
import app.cloudfit.accounts.infrastructure.adapter.output.email.LogEmailSender
import app.cloudfit.accounts.infrastructure.adapter.output.email.SmtpEmailSender
import app.cloudfit.accounts.infrastructure.adapter.output.identity.AppleJwksIdentityVerifier
import app.cloudfit.accounts.infrastructure.adapter.output.identity.GoogleJwksIdentityVerifier
import app.cloudfit.accounts.infrastructure.adapter.output.persistence.PostgresEmailTokenRepository
import app.cloudfit.accounts.infrastructure.adapter.output.persistence.PostgresIdentityRepository
import app.cloudfit.accounts.infrastructure.adapter.output.persistence.PostgresRefreshTokenRepository
import app.cloudfit.accounts.infrastructure.adapter.output.persistence.PostgresUserRepository
import app.cloudfit.accounts.infrastructure.adapter.output.security.Argon2PasswordHasher
import app.cloudfit.accounts.infrastructure.adapter.output.security.JwtTokenIssuer
import app.cloudfit.shared.application.port.ClockProvider
import app.cloudfit.shared.application.port.TransactionRunner
import app.cloudfit.shared.infrastructure.auth.JwtSettings
import io.ktor.server.routing.Route
import kotlinx.coroutines.CoroutineScope
import org.slf4j.LoggerFactory

class AccountsComponents(
    config: AccountsConfig,
    jwtSettings: JwtSettings,
    welcomeBonus: WelcomeBonusGranter,
    balances: AccountBalanceReader,
    avatars: AvatarUrlReader,
    limits: WardrobeLimitsReader,
    erasers: List<AccountDataEraser>,
    clock: ClockProvider,
    tx: TransactionRunner,
    scope: CoroutineScope,
) {
    private val users = PostgresUserRepository(clock)
    private val identities = PostgresIdentityRepository(clock)
    private val refreshTokens = PostgresRefreshTokenRepository()
    private val emailTokens = PostgresEmailTokenRepository()
    private val hasher = Argon2PasswordHasher()
    private val google = GoogleJwksIdentityVerifier(config.googleClientId)
    private val apple = AppleJwksIdentityVerifier(config.appleClientId)
    private val emailSender = if (config.smtp.configured) {
        SmtpEmailSender(config.smtp, scope)
    } else {
        LoggerFactory.getLogger("AccountsModule").warn("SMTP_HOST not set: emails are written to the log (dev only)")
        LogEmailSender()
    }
    private val mailer = AccountMailer(emailSender, config.policy.appUrl)
    private val tokenFactory = EmailTokenFactory(emailTokens, clock)
    private val meAssembler = MeAssembler(identities, balances, avatars, limits)
    private val sessions = SessionIssuer(refreshTokens, JwtTokenIssuer(jwtSettings, clock), meAssembler, config.policy, clock)
    private val cookie = RefreshCookieSettings(secure = config.cookieSecure)

    val getAccountProfile = GetAccountProfileService(users)

    private val authUseCases = AuthUseCases(
        providers = GetAuthProvidersService(google, apple, config.policy),
        register = RegisterService(users, hasher, tokenFactory, mailer, config.policy, tx, clock),
        verifyEmail = VerifyEmailService(emailTokens, users, welcomeBonus, sessions, tx, clock),
        resendVerification = ResendVerificationService(users, tokenFactory, mailer, config.policy),
        login = LoginService(users, hasher, sessions),
        socialLogin = SocialLoginService(google, apple, users, identities, welcomeBonus, sessions, tx, clock),
        refresh = RefreshSessionService(refreshTokens, users, sessions, tx, clock, config.policy),
        logout = LogoutService(refreshTokens, tx, clock),
        forgotPassword = ForgotPasswordService(users, tokenFactory, mailer, config.policy),
        resetPassword = ResetPasswordService(emailTokens, users, refreshTokens, hasher, welcomeBonus, tx, clock),
    )
    private val getMe = GetMeService(users, meAssembler)
    private val updateMe = UpdateMeService(users, meAssembler)
    private val deleteAccount = DeleteAccountService(users, erasers)

    fun registerRoutes(route: Route) {
        route.registerAuthRoutes(authUseCases, cookie)
        route.registerMeRoutes(getMe, updateMe, deleteAccount, cookie)
    }
}

fun Route.configureAccountsServiceRoutes(components: AccountsComponents) {
    components.registerRoutes(this)
}
