package app.cloudfit.wardrobe.application.port.output

import app.cloudfit.shared.application.port.StoredImage
import java.util.UUID

interface UserImageRepository {
    suspend fun record(userId: UUID, image: StoredImage)

    suspend fun listKeys(userId: UUID): List<String>
}
