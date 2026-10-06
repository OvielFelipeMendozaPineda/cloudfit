package app.cloudfit.accounts.infrastructure.adapter.output.persistence

import app.cloudfit.accounts.application.port.output.UserRepository
import app.cloudfit.accounts.domain.User
import app.cloudfit.shared.application.port.ClockProvider
import app.cloudfit.shared.domain.AppLocale
import app.cloudfit.shared.infrastructure.database.dbQuery
import app.cloudfit.shared.infrastructure.database.toUtc
import java.util.UUID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class PostgresUserRepository(private val clock: ClockProvider) : UserRepository {
    override suspend fun findById(id: UUID): User? = dbQuery {
        UsersTable.selectAll().where { UsersTable.id eq id }.map(::toUser).singleOrNull()
    }

    override suspend fun findByEmail(email: String): User? = dbQuery {
        UsersTable.selectAll().where { UsersTable.email.lowerCase() eq email.lowercase() }.map(::toUser).singleOrNull()
    }

    override suspend fun create(user: User) {
        dbQuery {
            UsersTable.insert {
                it[id] = user.id
                it[email] = user.email.lowercase()
                it[passwordHash] = user.passwordHash
                it[emailVerifiedAt] = user.emailVerifiedAt?.toUtc()
                it[displayName] = user.displayName
                it[locale] = user.locale.code
                it[createdAt] = user.createdAt.toUtc()
                it[updatedAt] = clock.now().toUtc()
            }
        }
    }

    override suspend fun update(user: User) {
        dbQuery {
            UsersTable.update({ UsersTable.id eq user.id }) {
                it[passwordHash] = user.passwordHash
                it[emailVerifiedAt] = user.emailVerifiedAt?.toUtc()
                it[displayName] = user.displayName
                it[locale] = user.locale.code
                it[updatedAt] = clock.now().toUtc()
            }
        }
    }

    override suspend fun delete(id: UUID) {
        dbQuery { UsersTable.deleteWhere { UsersTable.id eq id } }
    }

    private fun toUser(row: ResultRow) = User(
        id = row[UsersTable.id],
        email = row[UsersTable.email],
        passwordHash = row[UsersTable.passwordHash],
        emailVerifiedAt = row[UsersTable.emailVerifiedAt]?.toInstant(),
        displayName = row[UsersTable.displayName],
        locale = AppLocale.parse(row[UsersTable.locale]) ?: AppLocale.EN,
        createdAt = row[UsersTable.createdAt].toInstant(),
    )
}
