package com.masabi.cloudfit.storage

import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

/**
 * Where generated/uploaded images live. [LocalImageStore] writes to the local disk; Samuel swaps
 * it for an S3-backed implementation without touching the renderer.
 */
interface ImageStore {
    /** Persists the bytes and returns a URL that [get] can read back. */
    suspend fun put(bytes: ByteArray, contentType: String): String
    suspend fun get(url: String): ByteArray
}

/**
 * Saves images to a local directory (defaults to the user's Downloads folder) and returns a
 * `file://` URL. Good enough for the demo; no cloud needed.
 */
class LocalImageStore(private val dir: Path) : ImageStore {

    init {
        Files.createDirectories(dir)
    }

    override suspend fun put(bytes: ByteArray, contentType: String): String {
        val ext = when (contentType) {
            "image/jpeg" -> "jpg"
            "image/webp" -> "webp"
            else -> "png"
        }
        val file = dir.resolve("cloudfit-${UUID.randomUUID()}.$ext")
        Files.write(file, bytes)
        return file.toUri().toString()
    }

    override suspend fun get(url: String): ByteArray = Files.readAllBytes(Path.of(URI.create(url)))
}
