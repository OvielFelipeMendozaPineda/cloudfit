package app.cloudfit.wardrobe.infrastructure.adapter.output.persistence

import app.cloudfit.shared.application.port.ClockProvider
import app.cloudfit.shared.infrastructure.database.dbQuery
import app.cloudfit.shared.infrastructure.database.toUtc
import app.cloudfit.wardrobe.application.port.output.AvatarRepository
import java.util.UUID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.upsert

class PostgresAvatarRepository(private val clock: ClockProvider) : AvatarRepository {
    override suspend fun find(userId: UUID): String? = dbQuery {
        AvatarsTable.selectAll()
            .where { AvatarsTable.userId eq userId }
            .map { it[AvatarsTable.photoUrl] }
            .singleOrNull()
    }

    override suspend fun upsert(userId: UUID, photoUrl: String) {
        dbQuery {
            AvatarsTable.upsert {
                it[AvatarsTable.userId] = userId
                it[AvatarsTable.photoUrl] = photoUrl
                it[updatedAt] = clock.now().toUtc()
            }
        }
    }

    override suspend fun delete(userId: UUID): Boolean = dbQuery {
        AvatarsTable.deleteWhere { AvatarsTable.userId eq userId } > 0
    }
}
