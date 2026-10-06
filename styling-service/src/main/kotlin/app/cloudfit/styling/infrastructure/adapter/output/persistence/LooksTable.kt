package app.cloudfit.styling.infrastructure.adapter.output.persistence

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.UUIDColumnType
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object LooksTable : Table("looks") {
    val id = uuid("id")
    val userId = uuid("user_id")
    val event = varchar("event", 200)
    val status = varchar("status", 16)
    val clotheIds = array("clothe_ids", UUIDColumnType())
    val stylistNote = text("stylist_note").nullable()
    val imageUrl = text("image_url").nullable()
    val saved = bool("saved")
    val failureCode = varchar("failure_code", 32).nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(id)
}
