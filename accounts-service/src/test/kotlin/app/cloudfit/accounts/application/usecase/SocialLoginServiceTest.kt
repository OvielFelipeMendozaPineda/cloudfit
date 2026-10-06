package app.cloudfit.accounts.application.usecase

import app.cloudfit.accounts.application.port.input.RegisterCommand
import app.cloudfit.accounts.domain.AuthProvider
import app.cloudfit.accounts.domain.ExternalIdentity
import app.cloudfit.accounts.support.AccountsTestKit
import app.cloudfit.shared.application.error.ConflictException
import app.cloudfit.shared.application.error.ProviderNotConfiguredException
import app.cloudfit.shared.application.error.UnauthorizedException
import app.cloudfit.shared.domain.AppLocale
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

class SocialLoginServiceTest : StringSpec({

    fun identity(subject: String, email: String, verified: Boolean = true) =
        ExternalIdentity(AuthProvider.GOOGLE, subject, email, verified, "Ana G", "es-CO")

    "first Google sign-in creates a verified account and grants the welcome bonus once" {
        val kit = AccountsTestKit(mapOf("tok" to identity("g-1", "ana@gmail.com")))

        val first = kit.socialLogin.execute(AuthProvider.GOOGLE, "tok", null)
        val second = kit.socialLogin.execute(AuthProvider.GOOGLE, "tok", null)

        first.me.id shouldBe second.me.id
        first.me.emailVerified shouldBe true
        first.me.locale shouldBe AppLocale.ES
        first.me.displayName shouldBe "Ana G"
        kit.bonuses.size shouldBe 1
    }

    "a verified provider email links to the existing password account" {
        val kit = AccountsTestKit(mapOf("tok" to identity("g-2", "ana@example.com")))
        kit.register.execute(RegisterCommand("ana@example.com", "supersecret1", "en", null))
        kit.verifyEmail.execute(kit.emails.lastToken())

        val session = kit.socialLogin.execute(AuthProvider.GOOGLE, "tok", null)

        session.me.providers shouldContainExactlyInAnyOrder listOf(AuthProvider.PASSWORD, AuthProvider.GOOGLE)
        kit.users.users.size shouldBe 1
    }

    "linking to an unverified local account drops its unverified password" {
        val kit = AccountsTestKit(mapOf("tok" to identity("g-3", "ana@example.com")))
        kit.register.execute(RegisterCommand("ana@example.com", "attackerpass", "en", null))

        val session = kit.socialLogin.execute(AuthProvider.GOOGLE, "tok", null)

        session.me.providers shouldBe listOf(AuthProvider.GOOGLE)
        shouldThrow<UnauthorizedException> { kit.login.execute("ana@example.com", "attackerpass") }
        kit.bonuses.size shouldBe 1
    }

    "an unverified provider email never takes over an existing account" {
        val kit = AccountsTestKit(mapOf("tok" to identity("g-4", "ana@example.com", verified = false)))
        kit.register.execute(RegisterCommand("ana@example.com", "supersecret1", "en", null))

        shouldThrow<ConflictException> { kit.socialLogin.execute(AuthProvider.GOOGLE, "tok", null) }
    }

    "invalid identity tokens are rejected" {
        val kit = AccountsTestKit()
        shouldThrow<UnauthorizedException> { kit.socialLogin.execute(AuthProvider.GOOGLE, "garbage", null) }
    }

    "providers without a client id answer PROVIDER_NOT_CONFIGURED" {
        val kit = AccountsTestKit(googleClientId = null)
        shouldThrow<ProviderNotConfiguredException> { kit.socialLogin.execute(AuthProvider.GOOGLE, "tok", null) }
        shouldThrow<ProviderNotConfiguredException> { kit.socialLogin.execute(AuthProvider.APPLE, "tok", null) }
        kit.authUseCases.providers.execute().googleClientId shouldBe null
    }
})
