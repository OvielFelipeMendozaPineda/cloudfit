package app.cloudfit.accounts.application.port.output

import app.cloudfit.accounts.domain.AuthProvider
import app.cloudfit.accounts.domain.ExternalIdentity
import java.util.UUID

interface IdentityRepository {
    suspend fun findUserId(provider: AuthProvider, subject: String): UUID?

    suspend fun listProviders(userId: UUID): List<AuthProvider>

    suspend fun link(userId: UUID, identity: ExternalIdentity)
}
