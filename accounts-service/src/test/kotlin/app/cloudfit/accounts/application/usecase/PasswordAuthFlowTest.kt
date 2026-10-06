package app.cloudfit.accounts.application.usecase

import app.cloudfit.accounts.application.port.input.RegisterCommand
import app.cloudfit.accounts.domain.AuthProvider
import app.cloudfit.accounts.support.AccountsTestKit
import app.cloudfit.shared.application.error.ErrorCodes
import app.cloudfit.shared.application.error.ForbiddenException
import app.cloudfit.shared.application.error.UnauthorizedException
import app.cloudfit.shared.application.error.ValidationException
import app.cloudfit.shared.domain.AppLocale
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import java.time.Duration

class PasswordAuthFlowTest : StringSpec({

    suspend fun AccountsTestKit.registerAndVerify(email: String = "ana@example.com", password: String = "supersecret1") =
        register.execute(RegisterCommand(email, password, "es", "Ana")).let { verifyEmail.execute(emails.lastToken()) }

    "register sends a localized verification email and verify returns a session with the welcome bonus" {
        val kit = AccountsTestKit()

        kit.register.execute(RegisterCommand("Ana@Example.com", "supersecret1", "es", "Ana"))

        val email = kit.emails.sent.single()
        email.to shouldBe "ana@example.com"
        email.subject shouldBe "Verifica tu correo de CloudFit"
        email.text shouldContain "https://app.test/verify-email?token="

        val session = kit.verifyEmail.execute(kit.emails.lastToken())
        session.me.emailVerified shouldBe true
        session.me.locale shouldBe AppLocale.ES
        session.me.providers shouldContainExactly listOf(AuthProvider.PASSWORD)
        kit.bonuses shouldContainExactly listOf(session.me.id)
        session.refreshToken shouldNotBe ""
    }

    "verification tokens are single use" {
        val kit = AccountsTestKit()
        kit.register.execute(RegisterCommand("ana@example.com", "supersecret1", "en", null))
        val token = kit.emails.lastToken()
        kit.verifyEmail.execute(token)

        val error = shouldThrow<UnauthorizedException> { kit.verifyEmail.execute(token) }
        error.code shouldBe ErrorCodes.INVALID_TOKEN
    }

    "register never reveals existing accounts" {
        val kit = AccountsTestKit()
        kit.registerAndVerify()

        kit.register.execute(RegisterCommand("ana@example.com", "anotherpass1", "en", null))

        kit.emails.sent.last().subject shouldBe "Ya tienes una cuenta en CloudFit"
        kit.login.execute("ana@example.com", "supersecret1").me.email shouldBe "ana@example.com"
    }

    "register validates email, password length and locale" {
        val kit = AccountsTestKit()
        shouldThrow<ValidationException> { kit.register.execute(RegisterCommand("nope", "supersecret1", "en", null)) }
        shouldThrow<ValidationException> { kit.register.execute(RegisterCommand("a@b.co", "short", "en", null)) }
        shouldThrow<ValidationException> { kit.register.execute(RegisterCommand("a@b.co", "x".repeat(129), "en", null)) }
        shouldThrow<ValidationException> { kit.register.execute(RegisterCommand("a@b.co", "supersecret1", "fr", null)) }
    }

    "login rejects unverified accounts with EMAIL_NOT_VERIFIED and bad passwords with INVALID_CREDENTIALS" {
        val kit = AccountsTestKit()
        kit.register.execute(RegisterCommand("ana@example.com", "supersecret1", "en", null))

        shouldThrow<ForbiddenException> { kit.login.execute("ana@example.com", "supersecret1") }.code shouldBe ErrorCodes.EMAIL_NOT_VERIFIED
        shouldThrow<UnauthorizedException> { kit.login.execute("ana@example.com", "wrongpass1") }.code shouldBe ErrorCodes.INVALID_CREDENTIALS
        shouldThrow<UnauthorizedException> { kit.login.execute("ghost@example.com", "supersecret1") }.code shouldBe ErrorCodes.INVALID_CREDENTIALS
    }

    "refresh rotates the token and reusing an old one revokes the whole family" {
        val kit = AccountsTestKit()
        val first = kit.registerAndVerify()

        val second = kit.refresh.execute(first.refreshToken)
        second.refreshToken shouldNotBe first.refreshToken
        val third = kit.refresh.execute(second.refreshToken)
        kit.clock.advance(Duration.ofSeconds(16))

        shouldThrow<UnauthorizedException> { kit.refresh.execute(first.refreshToken) }.code shouldBe ErrorCodes.INVALID_TOKEN
        shouldThrow<UnauthorizedException> { kit.refresh.execute(third.refreshToken) }
        kit.refreshTokens.tokens.values.all { it.revokedAt != null } shouldBe true
    }

    "a concurrent refresh within the grace window gets its own session instead of revoking the family" {
        val kit = AccountsTestKit()
        val first = kit.registerAndVerify()

        val tabA = kit.refresh.execute(first.refreshToken)
        kit.clock.advance(Duration.ofSeconds(5))
        val tabB = kit.refresh.execute(first.refreshToken)

        tabB.refreshToken shouldNotBe tabA.refreshToken
        kit.refresh.execute(tabA.refreshToken).refreshToken shouldNotBe ""
        kit.refresh.execute(tabB.refreshToken).refreshToken shouldNotBe ""
    }

    "reuse within the grace window after logout is still rejected" {
        val kit = AccountsTestKit()
        val first = kit.registerAndVerify()
        val second = kit.refresh.execute(first.refreshToken)
        kit.logout.execute(second.refreshToken)

        shouldThrow<UnauthorizedException> { kit.refresh.execute(first.refreshToken) }
    }

    "refresh rejects missing, unknown and expired tokens" {
        val kit = AccountsTestKit()
        val session = kit.registerAndVerify()

        shouldThrow<UnauthorizedException> { kit.refresh.execute(null) }
        shouldThrow<UnauthorizedException> { kit.refresh.execute("unknown") }
        kit.clock.advance(Duration.ofDays(31))
        shouldThrow<UnauthorizedException> { kit.refresh.execute(session.refreshToken) }
    }

    "logout revokes the session family" {
        val kit = AccountsTestKit()
        val session = kit.registerAndVerify()

        kit.logout.execute(session.refreshToken)

        shouldThrow<UnauthorizedException> { kit.refresh.execute(session.refreshToken) }
    }

    "password reset changes the password and revokes every session" {
        val kit = AccountsTestKit()
        val session = kit.registerAndVerify()

        kit.forgotPassword.execute("ana@example.com")
        kit.emails.sent.last().text shouldContain "https://app.test/reset-password?token="
        kit.resetPassword.execute(kit.emails.lastToken(), "brandnewpass1")

        shouldThrow<UnauthorizedException> { kit.refresh.execute(session.refreshToken) }
        shouldThrow<UnauthorizedException> { kit.login.execute("ana@example.com", "supersecret1") }
        kit.login.execute("ana@example.com", "brandnewpass1").me.email shouldBe "ana@example.com"
    }

    "forgot password and resend verification are silent for unknown emails" {
        val kit = AccountsTestKit()
        kit.forgotPassword.execute("ghost@example.com")
        kit.resend.execute("ghost@example.com")
        kit.resend.execute("not-an-email")
        kit.emails.sent shouldBe emptyList()
    }

    "re-registering an unverified account sets the password chosen in the clicked link" {
        val kit = AccountsTestKit()
        kit.register.execute(RegisterCommand("ana@example.com", "firstpass1", "en", null))
        val firstToken = kit.emails.lastToken()
        kit.register.execute(RegisterCommand("ana@example.com", "secondpass1", "en", null))

        kit.verifyEmail.execute(firstToken)

        kit.login.execute("ana@example.com", "firstpass1").me.emailVerified shouldBe true
        shouldThrow<UnauthorizedException> { kit.login.execute("ana@example.com", "secondpass1") }
    }
})
