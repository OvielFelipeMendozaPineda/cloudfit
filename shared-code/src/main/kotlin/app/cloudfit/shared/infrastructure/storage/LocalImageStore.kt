package app.cloudfit.shared.infrastructure.storage

import app.cloudfit.shared.application.port.ImageContent
import app.cloudfit.shared.application.port.ImageFormats
import app.cloudfit.shared.application.port.ImageStore
import app.cloudfit.shared.application.port.StoredImage
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalImageStore(
    private val dir: Path,
    private val publicPath: String = "/api/v1/images",
) : ImageStore {

    init {
        Files.createDirectories(dir)
    }

    override suspend fun put(bytes: ByteArray, contentType: String): StoredImage = withContext(Dispatchers.IO) {
        val key = ImageFormats.newKey(contentType)
        Files.write(dir.resolve(key), bytes)
        StoredImage(key = key, url = "$publicPath/$key")
    }

    override suspend fun read(url: String): ImageContent {
        val key = url.removePrefix("$publicPath/")
        require(key != url) { "URL $url does not belong to the local image store" }
        return readByKey(key) ?: throw NoSuchElementException("Image $key not found")
    }

    override suspend fun readByKey(key: String): ImageContent? = withContext(Dispatchers.IO) {
        if (!ImageFormats.isValidKey(key)) return@withContext null
        val file = dir.resolve(key)
        if (!Files.exists(file)) return@withContext null
        ImageContent(Files.readAllBytes(file), ImageFormats.contentTypeOf(key))
    }

    override suspend fun delete(key: String) {
        if (!ImageFormats.isValidKey(key)) return
        withContext(Dispatchers.IO) { Files.deleteIfExists(dir.resolve(key)) }
    }
}
