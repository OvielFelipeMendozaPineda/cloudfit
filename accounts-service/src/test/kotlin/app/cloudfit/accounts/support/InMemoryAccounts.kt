package app.cloudfit.accounts.support

import app.cloudfit.accounts.application.port.output.AppleIdentityVerifier
import app.cloudfit.accounts.application.port.output.EmailMessage
import app.cloudfit.accounts.application.port.output.EmailSender
import app.cloudfit.accounts.application.port.output.EmailTokenRepository
import app.cloudfit.accounts.application.port.output.GoogleIdentityVerifier
import app.cloudfit.accounts.application.port.output.IdentityRepository
import app.cloudfit.accounts.application.port.output.PasswordHasher
import app.cloudfit.accounts.application.port.output.RefreshTokenRepository
import app.cloudfit.accounts.application.port.output.TokenIssuer
import app.cloudfit.accounts.application.port.output.UserRepository
import app.cloudfit.accounts.domain.AccessToken
import app.cloudfit.accounts.domain.AuthProvider
import app.cloudfit.accounts.domain.EmailToken
import app.cloudfit.accounts.domain.EmailTokenPurpose
import app.cloudfit.accounts.domain.ExternalIdentity
import app.cloudfit.accounts.domain.RefreshToken
import app.cloudfit.accounts.domain.User
import app.cloudfit.shared.application.error.ErrorCodes
import app.cloudfit.shared.application.error.UnauthorizedException
import java.time.Instant
import java.util.UUID

class InMemoryUserRepository : UserRepository {
    val users = mutableMapOf<UUID, User>()

    override suspend fun findById(id: UUID): User? = users[id]

    override suspend fun findByEmail(email: String): User? = users.values.firstOrNull { it.email.equals(email, ignoreCase = true) }

    override suspend fun create(user: User) {
        check(findByEmail(user.email) == null) { "duplicate email" }
        users[user.id] = user
    }

    override suspend fun update(user: User) {
        users[user.id] = user
    }

    override suspend fun delete(id: UUID) {
        users.remove(id)
    }
}

class InMemoryIdentityRepository : IdentityRepository {
    val identities = mutableListOf<Pair<UUID, ExternalIdentity>>()

    override suspend fun findUserId(provider: AuthProvider, subject: String): UUID? =
        identities.firstOrNull { it.second.provider == provider && it.second.subject == subject }?.first

    override suspend fun listProviders(userId: UUID): List<AuthProvider> =
        identities.filter { it.first == userId }.map { it.second.provider }

    override suspend fun link(userId: UUID, identity: ExternalIdentity) {
        if (findUserId(identity.provider, identity.subject) == null) identities += userId to identity
    }
}

class InMemoryRefreshTokenRepository : RefreshTokenRepository {
    val tokens = mutableMapOf<UUID, RefreshToken>()

    override suspend fun create(token: RefreshToken) {
        tokens[token.id] = token
    }

    override suspend fun lockByHash(tokenHash: String): RefreshToken? = tokens.values.firstOrNull { it.tokenHash == tokenHash }

    override suspend fun markReplaced(id: UUID, replacedBy: UUID, at: Instant) {
        tokens.computeIfPresent(id) { _, t -> t.copy(replacedBy = replacedBy, revokedAt = at) }
    }

    override suspend fun revokeFamily(familyId: UUID, at: Instant) {
        tokens.replaceAll { _, t -> if (t.familyId == familyId && t.revokedAt == null) t.copy(revokedAt = at) else t }
    }

    override suspend fun revokeAllForUser(userId: UUID, at: Instant) {
        tokens.replaceAll { _, t -> if (t.userId == userId && t.revokedAt == null) t.copy(revokedAt = at) else t }
    }
}

class InMemoryEmailTokenRepository : EmailTokenRepository {
    val tokens = mutableMapOf<UUID, EmailToken>()

    override suspend fun create(token: EmailToken) {
        tokens[token.id] = token
    }

    override suspend fun lockValid(tokenHash: String, purpose: EmailTokenPurpose, now: Instant): EmailToken? =
        tokens.values.firstOrNull { it.tokenHash == tokenHash && it.purpose == purpose && it.usedAt == null && it.expiresAt.isAfter(now) }

    override suspend fun markUsed(id: UUID, at: Instant) {
        tokens.computeIfPresent(id) { _, t -> t.copy(usedAt = at) }
    }

    override suspend fun invalidateAll(userId: UUID, purpose: EmailTokenPurpose, at: Instant) {
        tokens.replaceAll { _, t -> if (t.userId == userId && t.purpose == purpose && t.usedAt == null) t.copy(usedAt = at) else t }
    }
}

class FakePasswordHasher : PasswordHasher {
    override suspend fun hash(password: String): String = "hashed:$password"

    override suspend fun verify(password: String, hash: String): Boolean = hash == "hashed:$password"
}

class FakeTokenIssuer : TokenIssuer {
    override fun issueAccessToken(userId: UUID) = AccessToken("access-$userId-${UUID.randomUUID()}", 900)
}

class RecordingEmailSender : EmailSender {
    val sent = mutableListOf<EmailMessage>()

    override suspend fun send(message: EmailMessage) {
        sent += message
    }

    fun lastToken(): String = Regex("token=([A-Za-z0-9_%-]+)").find(sent.last().text)!!.groupValues[1]
}

class FakeIdentityVerifier(
    override val clientId: String?,
    private val identities: Map<String, ExternalIdentity> = emptyMap(),
) : GoogleIdentityVerifier, AppleIdentityVerifier {
    override suspend fun verify(idToken: String): ExternalIdentity =
        identities[idToken] ?: throw UnauthorizedException("bad token", ErrorCodes.INVALID_TOKEN)
}
