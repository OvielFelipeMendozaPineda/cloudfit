package com.masabi.cloudfit.outfit

import org.jetbrains.exposed.sql.Table

// clotheIds va como CSV en una sola columna — nada de tabla puente, no hace falta para el hackday.
object OutfitsTable : Table("outfits") {
    val id = varchar("id", 36)
    val event = varchar("event", 200)
    val clotheIds = varchar("clothe_ids", 500)
    val stylistNote = varchar("stylist_note", 500).nullable()
    val imageStatus = varchar("image_status", 20)
    val imageUrl = varchar("image_url", 500).nullable()

    override val primaryKey = PrimaryKey(id)
}
