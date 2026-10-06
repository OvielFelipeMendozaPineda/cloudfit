package app.cloudfit.shared.infrastructure.database

import app.cloudfit.shared.application.port.TransactionRunner
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class ExposedTransactionRunner : TransactionRunner {
    override suspend fun <T> inTransaction(block: suspend () -> T): T =
        if (TransactionManager.currentOrNull() != null) {
            block()
        } else {
            newSuspendedTransaction(Dispatchers.IO) { block() }
        }
}
