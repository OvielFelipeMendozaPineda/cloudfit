package app.cloudfit.billing.infrastructure.adapter.output.payment

import app.cloudfit.billing.application.port.output.WompiWebhookParser
import app.cloudfit.billing.domain.WompiTransactionEvent
import app.cloudfit.shared.application.error.ProviderNotConfiguredException
import app.cloudfit.shared.application.error.ValidationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class WompiWebhookVerifier(private val eventsSecret: String) : WompiWebhookParser {

    override fun parse(payload: String, checksumHeader: String?): WompiTransactionEvent? {
        if (eventsSecret.isBlank()) throw ProviderNotConfiguredException("WOMPI_EVENTS_SECRET is not set")
        val root = runCatching { Json.parseToJsonElement(payload) }.getOrElse { throw ValidationException("Invalid JSON") }
        val expected = checksumOf(root, eventsSecret)
        val provided = root.string("signature", "checksum") ?: checksumHeader
            ?: throw ValidationException("Missing Wompi checksum")
        if (!Crypto.constantTimeEquals(provided, expected)) throw ValidationException("Invalid Wompi checksum")
        if (root.string("event") != "transaction.updated") return null
        val transaction = root.at("data", "transaction") ?: throw ValidationException("Wompi event without transaction")
        return WompiTransactionEvent(
            transactionId = transaction.string("id") ?: throw ValidationException("Transaction without id"),
            reference = transaction.string("reference") ?: throw ValidationException("Transaction without reference"),
            status = transaction.string("status") ?: throw ValidationException("Transaction without status"),
            amountInCents = transaction.long("amount_in_cents") ?: throw ValidationException("Transaction without amount"),
            currency = transaction.string("currency") ?: "COP",
        )
    }

    companion object {
        fun checksumOf(root: JsonElement, secret: String): String {
            val properties = (root.at("signature", "properties") as? JsonArray)
                ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                ?: throw ValidationException("Wompi event without signature properties")
            val data = root.at("data")
            val values = properties.joinToString("") { property ->
                (data.at(*property.split('.').toTypedArray()) as? JsonPrimitive)?.content.orEmpty()
            }
            val timestamp = (root.at("timestamp") as? JsonPrimitive)?.content
                ?: throw ValidationException("Wompi event without timestamp")
            return Crypto.sha256Hex("$values$timestamp$secret")
        }
    }
}
