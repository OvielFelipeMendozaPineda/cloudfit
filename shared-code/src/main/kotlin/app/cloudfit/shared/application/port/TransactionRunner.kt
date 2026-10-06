package app.cloudfit.shared.application.port

interface TransactionRunner {
    suspend fun <T> inTransaction(block: suspend () -> T): T
}
