package app.cloudfit.wardrobe.infrastructure.adapter.output.persistence

import app.cloudfit.shared.application.port.ClockProvider
import app.cloudfit.shared.application.port.StoredImage
import app.cloudfit.shared.infrastructure.database.dbQuery
import app.cloudfit.shared.infrastructure.database.toUtc
import app.cloudfit.wardrobe.application.port.output.UserImageRepository
import java.util.UUID
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll

class PostgresUserImageRepository(private val clock: ClockProvider) : UserImageRepository {
    override suspend fun record(userId: UUID, image: StoredImage) {
        dbQuery {
            UserImagesTable.insertIgnore {
                it[key] = image.key
                it[UserImagesTable.userId] = userId
                it[url] = image.url
                it[createdAt] = clock.now().toUtc()
            }
        }
    }

    override suspend fun listKeys(userId: UUID): List<String> = dbQuery {
        UserImagesTable.selectAll().where { UserImagesTable.userId eq userId }.map { it[UserImagesTable.key] }
    }
}
