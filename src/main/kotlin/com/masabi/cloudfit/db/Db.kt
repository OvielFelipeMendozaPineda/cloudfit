package com.masabi.cloudfit.db

import com.masabi.cloudfit.avatar.AvatarTable
import com.masabi.cloudfit.clothes.ClothesTable
import com.masabi.cloudfit.config.DatabaseConfig
import com.masabi.cloudfit.outfit.OutfitsTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object Db {
    fun connect(config: DatabaseConfig): Database {
        val db = Database.connect(
            url = config.jdbcUrl,
            driver = "com.mysql.cj.jdbc.Driver",
            user = config.user,
            password = config.password,
        )
        transaction(db) {
            SchemaUtils.createMissingTablesAndColumns(ClothesTable, AvatarTable, OutfitsTable)
        }
        return db
    }
}
