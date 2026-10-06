package app.cloudfit.shared.testing

import app.cloudfit.shared.application.port.ImageContent
import app.cloudfit.shared.application.port.ImageFormats
import app.cloudfit.shared.application.port.ImageStore
import app.cloudfit.shared.application.port.StoredImage

class InMemoryImageStore(private val prefix: String = "/api/v1/images") : ImageStore {
    val images = linkedMapOf<String, ImageContent>()

    override suspend fun put(bytes: ByteArray, contentType: String): StoredImage {
        val key = ImageFormats.newKey(contentType)
        images[key] = ImageContent(bytes, contentType)
        return StoredImage(key, "$prefix/$key")
    }

    override suspend fun read(url: String): ImageContent =
        images[url.removePrefix("$prefix/")] ?: throw NoSuchElementException(url)

    override suspend fun readByKey(key: String): ImageContent? = images[key]

    override suspend fun delete(key: String) {
        images.remove(key)
    }
}
