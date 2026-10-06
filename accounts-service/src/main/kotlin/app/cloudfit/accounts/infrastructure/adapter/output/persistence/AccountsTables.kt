package app.cloudfit.accounts.infrastructure.adapter.output.persistence

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object UsersTable : Table("users") {
    val id = uuid("id")
    val email = text("email")
    val passwordHash = text("password_hash").nullable()
    val emailVerifiedAt = timestampWithTimeZone("email_verified_at").nullable()
    val displayName = varchar("display_name", 80).nullable()
    val locale = varchar("locale", 5)
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object UserIdentitiesTable : Table("user_identities") {
    val id = uuid("id")
    val userId = uuid("user_id")
    val provider = varchar("provider", 16)
    val subject = text("subject")
    val email = text("email").nullable()
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}

object RefreshTokensTable : Table("refresh_tokens") {
    val id = uuid("id")
    val userId = uuid("user_id")
    val familyId = uuid("family_id")
    val tokenHash = varchar("token_hash", 64)
    val expiresAt = timestampWithTimeZone("expires_at")
    val revokedAt = timestampWithTimeZone("revoked_at").nullable()
    val replacedBy = uuid("replaced_by").nullable()
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}

object EmailTokensTable : Table("email_tokens") {
    val id = uuid("id")
    val userId = uuid("user_id")
    val tokenHash = varchar("token_hash", 64)
    val purpose = varchar("purpose", 8)
    val passwordHash = text("password_hash").nullable()
    val expiresAt = timestampWithTimeZone("expires_at")
    val usedAt = timestampWithTimeZone("used_at").nullable()
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}
