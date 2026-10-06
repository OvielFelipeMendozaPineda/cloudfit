package app.cloudfit.accounts.infrastructure.adapter.output.identity

import app.cloudfit.shared.application.error.ErrorCodes
import app.cloudfit.shared.application.error.ProviderNotConfiguredException
import app.cloudfit.shared.application.error.UnauthorizedException
import com.auth0.jwk.Jwk
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.SigningKeyNotFoundException
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.util.Base64

class JwksIdentityVerifierTest : StringSpec({
    fun rsa(): KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

    val trusted = rsa()
    val attacker = rsa()

    fun jwkOf(keyId: String, key: RSAPublicKey): Jwk {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        fun unsigned(bytes: ByteArray) = if (bytes[0] == 0.toByte()) bytes.copyOfRange(1, bytes.size) else bytes
        return Jwk.fromValues(
            mapOf(
                "kid" to keyId,
                "kty" to "RSA",
                "alg" to "RS256",
                "use" to "sig",
                "n" to encoder.encodeToString(unsigned(key.modulus.toByteArray())),
                "e" to encoder.encodeToString(unsigned(key.publicExponent.toByteArray())),
            ),
        )
    }

    val jwks = JwkProvider { kid ->
        if (kid == "k1") jwkOf("k1", trusted.public as RSAPublicKey) else throw SigningKeyNotFoundException("no $kid", null)
    }

    fun token(
        keys: KeyPair = trusted,
        keyId: String = "k1",
        audience: String = "google-client",
        issuer: String = "https://accounts.google.com",
        expiresAt: Instant = Instant.now().plusSeconds(600),
        emailVerified: Any = true,
    ): String = JWT.create()
        .withKeyId(keyId)
        .withIssuer(issuer)
        .withAudience(audience)
        .withSubject("google-sub-1")
        .withClaim("email", "ana@gmail.com")
        .apply { if (emailVerified is Boolean) withClaim("email_verified", emailVerified) else withClaim("email_verified", emailVerified.toString()) }
        .withClaim("name", "Ana")
        .withExpiresAt(expiresAt)
        .sign(Algorithm.RSA256(keys.public as RSAPublicKey, keys.private as RSAPrivateKey))

    val google = GoogleJwksIdentityVerifier("google-client", jwks)

    "accepts a Google token signed by a published key" {
        val identity = google.verify(token())
        identity.subject shouldBe "google-sub-1"
        identity.email shouldBe "ana@gmail.com"
        identity.emailVerified shouldBe true
    }

    "accepts the legacy accounts.google.com issuer" {
        google.verify(token(issuer = "accounts.google.com")).subject shouldBe "google-sub-1"
    }

    "rejects garbage, wrong audience, wrong issuer, unknown key, forged signature and expired tokens" {
        val invalid = listOf(
            "not-a-jwt",
            token(audience = "someone-else"),
            token(issuer = "https://evil.example"),
            token(keyId = "k2"),
            token(keys = attacker),
            token(expiresAt = Instant.now().minusSeconds(3600)),
        )
        invalid.forEach { candidate ->
            shouldThrow<UnauthorizedException> { google.verify(candidate) }.code shouldBe ErrorCodes.INVALID_TOKEN
        }
    }

    "Apple tokens use the Apple issuer and string email_verified claims" {
        val apple = AppleJwksIdentityVerifier("apple-client", jwks)
        val identity = apple.verify(token(audience = "apple-client", issuer = "https://appleid.apple.com", emailVerified = "true"))
        identity.emailVerified shouldBe true
        shouldThrow<UnauthorizedException> { apple.verify(token(audience = "apple-client")) }
    }

    "verifiers without client id are not configured" {
        val verifier = GoogleJwksIdentityVerifier("", jwks)
        verifier.clientId shouldBe null
        shouldThrow<ProviderNotConfiguredException> { verifier.verify(token()) }
    }
})
