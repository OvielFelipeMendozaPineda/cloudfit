package app.cloudfit.wardrobe.infrastructure.adapter.output.removebg

import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode

class RemoveBgBackgroundRemoverTest : StringSpec({

    "sends the api key and returns the PNG body" {
        var apiKey: String? = null
        val client = HttpClient(MockEngine { request ->
            apiKey = request.headers["X-Api-Key"]
            respond(byteArrayOf(7, 7), HttpStatusCode.OK)
        })

        RemoveBgBackgroundRemover("key-1", client).remove(byteArrayOf(1), "image/jpeg").toList() shouldBe listOf<Byte>(7, 7)
        apiKey shouldBe "key-1"
    }

    "fails on non-2xx responses" {
        val client = HttpClient(MockEngine { respond("nope", HttpStatusCode.PaymentRequired) })
        shouldThrowAny { RemoveBgBackgroundRemover("key-1", client).remove(byteArrayOf(1), "image/jpeg") }
    }
})
