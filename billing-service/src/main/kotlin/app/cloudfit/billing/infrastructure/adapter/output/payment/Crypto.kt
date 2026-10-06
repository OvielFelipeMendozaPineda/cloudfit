package app.cloudfit.billing.infrastructure.adapter.output.payment

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal object Crypto {
    fun hmacSha256Hex(secret: String, payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        return mac.doFinal(payload.toByteArray()).toHex()
    }

    fun sha256Hex(payload: String): String =
        MessageDigest.getInstance("SHA-256").digest(payload.toByteArray()).toHex()

    fun constantTimeEquals(a: String, b: String): Boolean =
        MessageDigest.isEqual(a.lowercase().toByteArray(), b.lowercase().toByteArray())

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
