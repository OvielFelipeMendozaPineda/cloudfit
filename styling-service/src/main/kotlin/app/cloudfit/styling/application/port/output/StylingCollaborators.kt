package app.cloudfit.styling.application.port.output

import app.cloudfit.shared.domain.AppLocale
import app.cloudfit.styling.domain.WardrobeSnapshot
import java.util.UUID

fun interface WardrobeReader {
    suspend fun snapshot(userId: UUID): WardrobeSnapshot
}

fun interface UserLocaleReader {
    suspend fun locale(userId: UUID): AppLocale
}

interface CreditWallet {
    suspend fun reserve(userId: UUID, lookId: UUID)

    suspend fun confirm(lookId: UUID)

    suspend fun refund(lookId: UUID)
}

fun interface RenderedImageSaver {
    suspend fun save(userId: UUID, bytes: ByteArray, contentType: String): String
}

fun interface LookJobScheduler {
    fun schedule(lookId: UUID)
}
