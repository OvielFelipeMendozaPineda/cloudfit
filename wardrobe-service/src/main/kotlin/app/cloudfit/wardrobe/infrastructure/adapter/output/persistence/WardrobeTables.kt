package app.cloudfit.wardrobe.infrastructure.adapter.output.persistence

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object ClothesTable : Table("clothes") {
    val id = uuid("id")
    val userId = uuid("user_id")
    val category = varchar("category", 16)
    val imageUrl = text("image_url")
    val name = varchar("name", 100).nullable()
    val colour = varchar("colour", 50).nullable()
    val pattern = varchar("pattern", 50).nullable()
    val formality = varchar("formality", 16).nullable()
    val warmth = varchar("warmth", 16).nullable()
    val description = varchar("description", 500).nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object AvatarsTable : Table("avatars") {
    val userId = uuid("user_id")
    val photoUrl = text("photo_url")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(userId)
}

object UserImagesTable : Table("user_images") {
    val key = text("key")
    val userId = uuid("user_id")
    val url = text("url")
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(key)
}
