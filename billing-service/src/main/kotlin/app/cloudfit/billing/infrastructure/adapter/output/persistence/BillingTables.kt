package app.cloudfit.billing.infrastructure.adapter.output.persistence

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object WalletBucketsTable : Table("wallet_buckets") {
    val userId = uuid("user_id")
    val bucket = varchar("bucket", 8)
    val balance = integer("balance")
    val expiresAt = timestampWithTimeZone("expires_at").nullable()
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(userId, bucket)
}

object CreditLedgerTable : Table("credit_ledger") {
    val id = uuid("id")
    val userId = uuid("user_id")
    val delta = integer("delta")
    val bucket = varchar("bucket", 8)
    val reason = varchar("reason", 24)
    val refId = text("ref_id").nullable()
    val idempotencyKey = text("idempotency_key")
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}

object CreditHoldsTable : Table("credit_holds") {
    val refId = text("ref_id")
    val userId = uuid("user_id")
    val amount = integer("amount")
    val status = varchar("status", 10)
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(refId)
}

object BillingCustomersTable : Table("billing_customers") {
    val userId = uuid("user_id")
    val stripeCustomerId = text("stripe_customer_id")
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(userId)
}

object SubscriptionsTable : Table("subscriptions") {
    val id = uuid("id")
    val userId = uuid("user_id")
    val provider = varchar("provider", 8)
    val providerSubscriptionId = text("provider_subscription_id").nullable()
    val planCode = varchar("plan_code", 16)
    val status = varchar("status", 16)
    val currentPeriodEnd = timestampWithTimeZone("current_period_end").nullable()
    val cancelAtPeriodEnd = bool("cancel_at_period_end")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object PaymentsTable : Table("payments") {
    val id = uuid("id")
    val userId = uuid("user_id").nullable()
    val provider = varchar("provider", 8)
    val productCode = varchar("product_code", 16)
    val amount = long("amount")
    val currency = varchar("currency", 3)
    val status = varchar("status", 10)
    val providerRef = text("provider_ref").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object WebhookEventsTable : Table("webhook_events") {
    val provider = varchar("provider", 8)
    val eventId = text("event_id")
    val eventType = text("event_type")
    val receivedAt = timestampWithTimeZone("received_at")

    override val primaryKey = PrimaryKey(provider, eventId)
}
