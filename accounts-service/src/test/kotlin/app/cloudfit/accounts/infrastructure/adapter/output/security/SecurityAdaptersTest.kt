package app.cloudfit.accounts.infrastructure.adapter.output.security

import app.cloudfit.shared.infrastructure.auth.JwtSettings
import app.cloudfit.shared.testing.MutableClock
import com.auth0.jwt.JWT
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import java.util.UUID

class SecurityAdaptersTest : StringSpec({

    "argon2id hashes are salted and verifiable" {
        val hasher = Argon2PasswordHasher(memoryKib = 1024, iterations = 1)
        val first = hasher.hash("supersecret1")
        val second = hasher.hash("supersecret1")

        first shouldStartWith "\$argon2id\$"
        first shouldNotBe second
        hasher.verify("supersecret1", first) shouldBe true
        hasher.verify("wrong", first) shouldBe false
        hasher.verify("supersecret1", "not-a-hash") shouldBe false
    }

    "access tokens are HS256 JWTs with kid, subject and a 15 minute expiry" {
        val settings = JwtSettings(secret = "x".repeat(32), keyId = "v1", issuer = "cloudfit", audience = "cloudfit-api")
        val clock = MutableClock()
        val userId = UUID.randomUUID()

        val token = JwtTokenIssuer(settings, clock).issueAccessToken(userId)
        val decoded = JWT.decode(token.token)

        token.expiresInSeconds shouldBe 900
        decoded.keyId shouldBe "v1"
        decoded.algorithm shouldBe "HS256"
        decoded.subject shouldBe userId.toString()
        decoded.getClaim("typ").asString() shouldBe "access"
        (decoded.expiresAtAsInstant.epochSecond - clock.now().epochSecond) shouldBe 900
    }

    "a JWT secret shorter than 32 bytes refuses to start" {
        shouldThrow<IllegalStateException> { JwtSettings(secret = "short", keyId = "v1", issuer = "i", audience = "a") }
    }
})
