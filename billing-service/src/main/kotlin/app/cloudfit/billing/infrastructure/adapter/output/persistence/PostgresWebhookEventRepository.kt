package app.cloudfit.billing.infrastructure.adapter.output.persistence

import app.cloudfit.billing.application.port.output.WebhookEventRepository
import app.cloudfit.billing.domain.PaymentProvider
import app.cloudfit.shared.application.port.ClockProvider
import app.cloudfit.shared.infrastructure.database.dbQuery
import app.cloudfit.shared.infrastructure.database.toUtc
import org.jetbrains.exposed.sql.insertIgnore

class PostgresWebhookEventRepository(private val clock: ClockProvider) : WebhookEventRepository {
    override suspend fun registerIfNew(provider: PaymentProvider, eventId: String, type: String): Boolean = dbQuery {
        WebhookEventsTable.insertIgnore {
            it[WebhookEventsTable.provider] = provider.name
            it[WebhookEventsTable.eventId] = eventId
            it[eventType] = type
            it[receivedAt] = clock.now().toUtc()
        }.insertedCount > 0
    }
}
