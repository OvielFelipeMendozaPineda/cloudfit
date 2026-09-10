package com.masabi.cloudfit.clothes

import org.jetbrains.exposed.sql.Table

object ClothesTable : Table("clothes") {
    val id = varchar("id", 36)
    val category = varchar("category", 20)
    val imageUrl = varchar("image_url", 500)
    val name = varchar("name", 100).nullable()
    val colour = varchar("colour", 50).nullable()
    val pattern = varchar("pattern", 50).nullable()
    val formality = varchar("formality", 20).nullable()
    val warmth = varchar("warmth", 20).nullable()
    val description = varchar("description", 500).nullable()

    override val primaryKey = PrimaryKey(id)
}
