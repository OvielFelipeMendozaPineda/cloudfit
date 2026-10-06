package app.cloudfit.billing.infrastructure.adapter.output.persistence

import app.cloudfit.billing.application.port.output.CreditHoldRepository
import app.cloudfit.billing.domain.CreditHold
import app.cloudfit.billing.domain.HoldStatus
import app.cloudfit.shared.application.port.ClockProvider
import app.cloudfit.shared.infrastructure.database.dbQuery
import app.cloudfit.shared.infrastructure.database.toUtc
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class PostgresCreditHoldRepository(private val clock: ClockProvider) : CreditHoldRepository {
    override suspend fun create(hold: CreditHold) {
        dbQuery {
            val now = clock.now().toUtc()
            CreditHoldsTable.insert {
                it[refId] = hold.refId
                it[userId] = hold.userId
                it[amount] = hold.amount
                it[status] = hold.status.name
                it[createdAt] = now
                it[updatedAt] = now
            }
        }
    }

    override suspend fun lock(refId: String): CreditHold? = dbQuery {
        CreditHoldsTable.selectAll()
            .where { CreditHoldsTable.refId eq refId }
            .forUpdate()
            .map {
                CreditHold(
                    refId = it[CreditHoldsTable.refId],
                    userId = it[CreditHoldsTable.userId],
                    amount = it[CreditHoldsTable.amount],
                    status = HoldStatus.valueOf(it[CreditHoldsTable.status]),
                )
            }
            .singleOrNull()
    }

    override suspend fun updateStatus(refId: String, status: HoldStatus) {
        dbQuery {
            CreditHoldsTable.update({ CreditHoldsTable.refId eq refId }) {
                it[CreditHoldsTable.status] = status.name
                it[updatedAt] = clock.now().toUtc()
            }
        }
    }
}
