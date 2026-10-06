package app.cloudfit.accounts.support

import app.cloudfit.accounts.application.usecase.AccountMailer
import app.cloudfit.accounts.application.usecase.EmailTokenFactory
import app.cloudfit.accounts.application.usecase.ForgotPasswordService
import app.cloudfit.accounts.application.usecase.GetAuthProvidersService
import app.cloudfit.accounts.application.usecase.LoginService
import app.cloudfit.accounts.application.usecase.LogoutService
import app.cloudfit.accounts.application.usecase.MeAssembler
import app.cloudfit.accounts.application.usecase.RefreshSessionService
import app.cloudfit.accounts.application.usecase.RegisterService
import app.cloudfit.accounts.application.usecase.ResendVerificationService
import app.cloudfit.accounts.application.usecase.ResetPasswordService
import app.cloudfit.accounts.application.usecase.SessionIssuer
import app.cloudfit.accounts.application.usecase.SocialLoginService
import app.cloudfit.accounts.application.usecase.VerifyEmailService
import app.cloudfit.accounts.domain.AccountBalance
import app.cloudfit.accounts.domain.AccountsPolicy
import app.cloudfit.accounts.domain.CreditsView
import app.cloudfit.accounts.domain.ExternalIdentity
import app.cloudfit.accounts.infrastructure.adapter.input.http.AuthUseCases
import app.cloudfit.shared.testing.MutableClock
import app.cloudfit.shared.testing.PassthroughTransactionRunner
import java.util.UUID

class AccountsTestKit(
    googleIdentities: Map<String, ExternalIdentity> = emptyMap(),
    googleClientId: String? = "google-client",
) {
    val clock = MutableClock()
    val tx = PassthroughTransactionRunner
    val policy = AccountsPolicy(appUrl = "https://app.test")
    val users = InMemoryUserRepository()
    val identities = InMemoryIdentityRepository()
    val refreshTokens = InMemoryRefreshTokenRepository()
    val emailTokens = InMemoryEmailTokenRepository()
    val hasher = FakePasswordHasher()
    val emails = RecordingEmailSender()
    val bonuses = mutableListOf<UUID>()
    val google = FakeIdentityVerifier(googleClientId, googleIdentities)
    val apple = FakeIdentityVerifier(null)

    private val mailer = AccountMailer(emails, policy.appUrl)
    private val tokenFactory = EmailTokenFactory(emailTokens, clock)
    val meAssembler = MeAssembler(
        identities = identities,
        balances = { AccountBalance(CreditsView(bonuses.count() * 5, 0, bonuses.count() * 5, 0, 0), null) },
        avatars = { null },
        limits = { 30 },
    )
    val sessions = SessionIssuer(refreshTokens, FakeTokenIssuer(), meAssembler, policy, clock)
    private val bonus = app.cloudfit.accounts.application.port.output.WelcomeBonusGranter { if (it !in bonuses) bonuses += it }

    val register = RegisterService(users, hasher, tokenFactory, mailer, policy, tx, clock)
    val resend = ResendVerificationService(users, tokenFactory, mailer, policy)
    val verifyEmail = VerifyEmailService(emailTokens, users, bonus, sessions, tx, clock)
    val login = LoginService(users, hasher, sessions)
    val socialLogin = SocialLoginService(google, apple, users, identities, bonus, sessions, tx, clock)
    val refresh = RefreshSessionService(refreshTokens, users, sessions, tx, clock, policy)
    val logout = LogoutService(refreshTokens, tx, clock)
    val forgotPassword = ForgotPasswordService(users, tokenFactory, mailer, policy)
    val resetPassword = ResetPasswordService(emailTokens, users, refreshTokens, hasher, bonus, tx, clock)

    val authUseCases = AuthUseCases(
        providers = GetAuthProvidersService(google, apple, policy),
        register = register,
        verifyEmail = verifyEmail,
        resendVerification = resend,
        login = login,
        socialLogin = socialLogin,
        refresh = refresh,
        logout = logout,
        forgotPassword = forgotPassword,
        resetPassword = resetPassword,
    )
}
