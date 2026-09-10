package com.masabi.cloudfit.avatar

import org.jetbrains.exposed.sql.Table

object AvatarTable : Table("avatar") {
    val id = integer("id")
    val photoUrl = varchar("photo_url", 500)

    override val primaryKey = PrimaryKey(id)
}
