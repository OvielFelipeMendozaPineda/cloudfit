package app.cloudfit.accounts.infrastructure.adapter.output.persistence

import app.cloudfit.accounts.application.port.output.EmailTokenRepository
import app.cloudfit.accounts.domain.EmailToken
import app.cloudfit.accounts.domain.EmailTokenPurpose
import app.cloudfit.shared.infrastructure.database.dbQuery
import app.cloudfit.shared.infrastructure.database.toUtc
import java.time.Instant
import java.util.UUID
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class PostgresEmailTokenRepository : EmailTokenRepository {
    override suspend fun create(token: EmailToken) {
        dbQuery {
            EmailTokensTable.insert {
                it[id] = token.id
                it[userId] = token.userId
                it[tokenHash] = token.tokenHash
                it[purpose] = token.purpose.name
                it[passwordHash] = token.passwordHash
                it[expiresAt] = token.expiresAt.toUtc()
                it[usedAt] = token.usedAt?.toUtc()
                it[createdAt] = Instant.now().toUtc()
            }
        }
    }

    override suspend fun lockValid(tokenHash: String, purpose: EmailTokenPurpose, now: Instant): EmailToken? = dbQuery {
        EmailTokensTable.selectAll()
            .where {
                (EmailTokensTable.tokenHash eq tokenHash) and
                    (EmailTokensTable.purpose eq purpose.name) and
                    EmailTokensTable.usedAt.isNull() and
                    (EmailTokensTable.expiresAt greater now.toUtc())
            }
            .forUpdate()
            .map {
                EmailToken(
                    id = it[EmailTokensTable.id],
                    userId = it[EmailTokensTable.userId],
                    tokenHash = it[EmailTokensTable.tokenHash],
                    purpose = EmailTokenPurpose.valueOf(it[EmailTokensTable.purpose]),
                    passwordHash = it[EmailTokensTable.passwordHash],
                    expiresAt = it[EmailTokensTable.expiresAt].toInstant(),
                    usedAt = it[EmailTokensTable.usedAt]?.toInstant(),
                )
            }
            .singleOrNull()
    }

    override suspend fun markUsed(id: UUID, at: Instant) {
        dbQuery {
            EmailTokensTable.update({ EmailTokensTable.id eq id }) { it[usedAt] = at.toUtc() }
        }
    }

    override suspend fun invalidateAll(userId: UUID, purpose: EmailTokenPurpose, at: Instant) {
        dbQuery {
            EmailTokensTable.update({
                (EmailTokensTable.userId eq userId) and (EmailTokensTable.purpose eq purpose.name) and EmailTokensTable.usedAt.isNull()
            }) { it[usedAt] = at.toUtc() }
        }
    }
}
