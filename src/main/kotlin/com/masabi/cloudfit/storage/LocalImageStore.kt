package com.masabi.cloudfit.storage

import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

class LocalImageStore(private val dir: Path) {

    init {
        Files.createDirectories(dir)
    }

    fun put(bytes: ByteArray, contentType: String): String {
        val ext = when (contentType) {
            "image/jpeg" -> "jpg"
            "image/webp" -> "webp"
            else -> "png"
        }
        val file = dir.resolve("cloudfit-${UUID.randomUUID()}.$ext")
        Files.write(file, bytes)
        return file.toUri().toString()
    }

    fun get(url: String): ByteArray = Files.readAllBytes(Path.of(URI.create(url)))
}
