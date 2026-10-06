package app.cloudfit.accounts.infrastructure.adapter.output.persistence

import app.cloudfit.accounts.application.port.output.RefreshTokenRepository
import app.cloudfit.accounts.domain.RefreshToken
import app.cloudfit.shared.infrastructure.database.dbQuery
import app.cloudfit.shared.infrastructure.database.toUtc
import java.time.Instant
import java.util.UUID
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class PostgresRefreshTokenRepository : RefreshTokenRepository {
    override suspend fun create(token: RefreshToken) {
        dbQuery {
            RefreshTokensTable.insert {
                it[id] = token.id
                it[userId] = token.userId
                it[familyId] = token.familyId
                it[tokenHash] = token.tokenHash
                it[expiresAt] = token.expiresAt.toUtc()
                it[revokedAt] = token.revokedAt?.toUtc()
                it[replacedBy] = token.replacedBy
                it[createdAt] = Instant.now().toUtc()
            }
        }
    }

    override suspend fun lockByHash(tokenHash: String): RefreshToken? = dbQuery {
        RefreshTokensTable.selectAll()
            .where { RefreshTokensTable.tokenHash eq tokenHash }
            .forUpdate()
            .map {
                RefreshToken(
                    id = it[RefreshTokensTable.id],
                    userId = it[RefreshTokensTable.userId],
                    familyId = it[RefreshTokensTable.familyId],
                    tokenHash = it[RefreshTokensTable.tokenHash],
                    expiresAt = it[RefreshTokensTable.expiresAt].toInstant(),
                    revokedAt = it[RefreshTokensTable.revokedAt]?.toInstant(),
                    replacedBy = it[RefreshTokensTable.replacedBy],
                )
            }
            .singleOrNull()
    }

    override suspend fun markReplaced(id: UUID, replacedBy: UUID, at: Instant) {
        dbQuery {
            RefreshTokensTable.update({ RefreshTokensTable.id eq id }) {
                it[RefreshTokensTable.replacedBy] = replacedBy
                it[revokedAt] = at.toUtc()
            }
        }
    }

    override suspend fun revokeFamily(familyId: UUID, at: Instant) {
        dbQuery {
            RefreshTokensTable.update({ (RefreshTokensTable.familyId eq familyId) and RefreshTokensTable.revokedAt.isNull() }) {
                it[revokedAt] = at.toUtc()
            }
        }
    }

    override suspend fun revokeAllForUser(userId: UUID, at: Instant) {
        dbQuery {
            RefreshTokensTable.update({ (RefreshTokensTable.userId eq userId) and RefreshTokensTable.revokedAt.isNull() }) {
                it[revokedAt] = at.toUtc()
            }
        }
    }
}
