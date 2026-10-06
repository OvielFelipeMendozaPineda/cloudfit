package app.cloudfit.wardrobe.support

import app.cloudfit.shared.application.port.StoredImage
import app.cloudfit.wardrobe.application.port.output.AvatarRepository
import app.cloudfit.wardrobe.application.port.output.ClothesRepository
import app.cloudfit.wardrobe.application.port.output.UserImageRepository
import app.cloudfit.wardrobe.domain.Clothe
import java.util.UUID

class InMemoryClothesRepository : ClothesRepository {
    val clothes = linkedMapOf<UUID, Clothe>()

    override suspend fun listByUser(userId: UUID): List<Clothe> = clothes.values.filter { it.userId == userId }

    override suspend fun find(userId: UUID, id: UUID): Clothe? = clothes[id]?.takeIf { it.userId == userId }

    override suspend fun countByUser(userId: UUID): Long = listByUser(userId).size.toLong()

    override suspend fun insert(clothe: Clothe) {
        clothes[clothe.id] = clothe
    }

    override suspend fun update(clothe: Clothe) {
        if (find(clothe.userId, clothe.id) != null) clothes[clothe.id] = clothe
    }

    override suspend fun delete(userId: UUID, id: UUID): Boolean = find(userId, id)?.let { clothes.remove(id) } != null
}

class InMemoryAvatarRepository : AvatarRepository {
    val avatars = mutableMapOf<UUID, String>()

    override suspend fun find(userId: UUID): String? = avatars[userId]

    override suspend fun upsert(userId: UUID, photoUrl: String) {
        avatars[userId] = photoUrl
    }

    override suspend fun delete(userId: UUID): Boolean = avatars.remove(userId) != null
}

class InMemoryUserImageRepository : UserImageRepository {
    val images = mutableListOf<Pair<UUID, StoredImage>>()

    override suspend fun record(userId: UUID, image: StoredImage) {
        images += userId to image
    }

    override suspend fun listKeys(userId: UUID): List<String> = images.filter { it.first == userId }.map { it.second.key }
}
