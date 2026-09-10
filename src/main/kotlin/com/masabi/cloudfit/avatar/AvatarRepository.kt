package com.masabi.cloudfit.avatar

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

private const val SINGLETON_ID = 1

class AvatarRepository {

    fun get(): Avatar? = transaction {
        AvatarTable.selectAll().where { AvatarTable.id eq SINGLETON_ID }
            .map { Avatar(it[AvatarTable.photoUrl]) }
            .singleOrNull()
    }

    fun set(avatar: Avatar): Avatar = transaction {
        val exists = AvatarTable.selectAll().where { AvatarTable.id eq SINGLETON_ID }.any()
        if (exists) {
            AvatarTable.update({ AvatarTable.id eq SINGLETON_ID }) { it[photoUrl] = avatar.photoUrl }
        } else {
            AvatarTable.insert {
                it[id] = SINGLETON_ID
                it[photoUrl] = avatar.photoUrl
            }
        }
        avatar
    }

    fun clear() {
        transaction { AvatarTable.deleteWhere { AvatarTable.id eq SINGLETON_ID } }
    }
}
