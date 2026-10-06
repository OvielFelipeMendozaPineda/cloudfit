package app.cloudfit.shared.application.port

data class StoredImage(
    val key: String,
    val url: String,
)

class ImageContent(
    val bytes: ByteArray,
    val contentType: String,
)

interface ImageStore {
    suspend fun put(bytes: ByteArray, contentType: String): StoredImage

    suspend fun read(url: String): ImageContent

    suspend fun readByKey(key: String): ImageContent?

    suspend fun delete(key: String)
}
