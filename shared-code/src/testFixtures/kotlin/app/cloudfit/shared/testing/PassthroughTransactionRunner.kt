package app.cloudfit.shared.testing

import app.cloudfit.shared.application.port.TransactionRunner

object PassthroughTransactionRunner : TransactionRunner {
    override suspend fun <T> inTransaction(block: suspend () -> T): T = block()
}
