package app.cloudfit.billing.infrastructure.adapter.output.payment

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

internal fun JsonElement?.at(vararg path: String): JsonElement? {
    var current: JsonElement? = this
    for (segment in path) {
        current = when (val node = current) {
            is JsonObject -> node[segment]
            is JsonArray -> segment.toIntOrNull()?.let { node.getOrNull(it) }
            else -> null
        }
    }
    return current?.takeUnless { it is JsonNull }
}

internal fun JsonElement?.string(vararg path: String): String? = (at(*path) as? JsonPrimitive)?.contentOrNull

internal fun JsonElement?.long(vararg path: String): Long? =
    (at(*path) as? JsonPrimitive)?.let { it.longOrNull ?: it.contentOrNull?.toLongOrNull() }

internal fun JsonElement?.bool(vararg path: String): Boolean? = (at(*path) as? JsonPrimitive)?.booleanOrNull
