package app.cloudfit.shared.infrastructure.database

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

suspend fun <T> dbQuery(block: () -> T): T =
    if (TransactionManager.currentOrNull() != null) {
        block()
    } else {
        newSuspendedTransaction(Dispatchers.IO) { block() }
    }
