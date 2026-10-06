package app.cloudfit.shared.infrastructure.storage

import app.cloudfit.shared.application.port.ImageFormats
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import java.nio.file.Files

class LocalImageStoreTest : StringSpec({

    "stores images under uuid keys and serves them by key and by url" {
        val store = LocalImageStore(Files.createTempDirectory("cloudfit-images"))

        val stored = store.put("PNGDATA".toByteArray(), "image/png")

        stored.url shouldStartWith "/api/v1/images/"
        ImageFormats.isValidKey(stored.key) shouldBe true
        store.readByKey(stored.key)!!.contentType shouldBe "image/png"
        store.read(stored.url).bytes.toList() shouldBe "PNGDATA".toByteArray().toList()

        store.delete(stored.key)
        store.readByKey(stored.key) shouldBe null
    }

    "refuses path traversal and foreign urls" {
        val store = LocalImageStore(Files.createTempDirectory("cloudfit-images"))
        store.readByKey("../../etc/passwd") shouldBe null
        store.readByKey("not-a-key.png") shouldBe null
        shouldThrowAny { store.read("https://elsewhere.example/a.png") }
    }

    "only common image types are accepted" {
        ImageFormats.isSupported("image/jpeg") shouldBe true
        ImageFormats.isSupported("image/png; charset=binary") shouldBe true
        ImageFormats.isSupported("application/pdf") shouldBe false
    }
})
