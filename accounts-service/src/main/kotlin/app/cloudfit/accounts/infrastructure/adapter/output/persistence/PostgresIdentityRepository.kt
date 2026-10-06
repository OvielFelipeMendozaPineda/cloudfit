package app.cloudfit.accounts.infrastructure.adapter.output.persistence

import app.cloudfit.accounts.application.port.output.IdentityRepository
import app.cloudfit.accounts.domain.AuthProvider
import app.cloudfit.accounts.domain.ExternalIdentity
import app.cloudfit.shared.application.port.ClockProvider
import app.cloudfit.shared.infrastructure.database.dbQuery
import app.cloudfit.shared.infrastructure.database.toUtc
import java.util.UUID
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll

class PostgresIdentityRepository(private val clock: ClockProvider) : IdentityRepository {
    override suspend fun findUserId(provider: AuthProvider, subject: String): UUID? = dbQuery {
        UserIdentitiesTable.selectAll()
            .where { (UserIdentitiesTable.provider eq provider.name) and (UserIdentitiesTable.subject eq subject) }
            .map { it[UserIdentitiesTable.userId] }
            .singleOrNull()
    }

    override suspend fun listProviders(userId: UUID): List<AuthProvider> = dbQuery {
        UserIdentitiesTable.selectAll()
            .where { UserIdentitiesTable.userId eq userId }
            .map { AuthProvider.valueOf(it[UserIdentitiesTable.provider]) }
    }

    override suspend fun link(userId: UUID, identity: ExternalIdentity) {
        dbQuery {
            UserIdentitiesTable.insertIgnore {
                it[id] = UUID.randomUUID()
                it[UserIdentitiesTable.userId] = userId
                it[provider] = identity.provider.name
                it[subject] = identity.subject
                it[email] = identity.email
                it[createdAt] = clock.now().toUtc()
            }
        }
    }
}
