package app.cloudfit.shared.application.port

import java.util.UUID

object ImageFormats {
    private val extensionsByType = mapOf(
        "image/png" to "png",
        "image/jpeg" to "jpg",
        "image/webp" to "webp",
        "image/heic" to "heic",
    )
    private val keyPattern = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(png|jpg|webp|heic)$")

    fun isSupported(contentType: String): Boolean = normalize(contentType) in extensionsByType

    fun newKey(contentType: String): String {
        val ext = extensionsByType[normalize(contentType)] ?: "png"
        return "${UUID.randomUUID()}.$ext"
    }

    fun isValidKey(key: String): Boolean = keyPattern.matches(key)

    fun contentTypeOf(key: String): String = when (key.substringAfterLast('.', "").lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "webp" -> "image/webp"
        "heic" -> "image/heic"
        else -> "image/png"
    }

    private fun normalize(contentType: String): String = contentType.substringBefore(';').trim().lowercase()
}
