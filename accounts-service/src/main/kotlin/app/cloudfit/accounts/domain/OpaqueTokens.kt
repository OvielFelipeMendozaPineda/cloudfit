package app.cloudfit.accounts.domain

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object OpaqueTokens {
    private const val TOKEN_BYTES = 32
    private val random = SecureRandom()

    fun generate(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun hash(token: String): String =
        MessageDigest.getInstance("SHA-256").digest(token.toByteArray()).joinToString("") { "%02x".format(it) }
}
