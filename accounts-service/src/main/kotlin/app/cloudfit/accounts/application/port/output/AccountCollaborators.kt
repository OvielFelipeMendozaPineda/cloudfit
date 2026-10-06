package app.cloudfit.accounts.application.port.output

import app.cloudfit.accounts.domain.AccountBalance
import java.util.UUID

fun interface WelcomeBonusGranter {
    suspend fun grant(userId: UUID)
}

fun interface AccountBalanceReader {
    suspend fun read(userId: UUID): AccountBalance
}

fun interface AvatarUrlReader {
    suspend fun avatarUrl(userId: UUID): String?
}

fun interface WardrobeLimitsReader {
    suspend fun maxClothes(userId: UUID): Int?
}

fun interface AccountDataEraser {
    suspend fun erase(userId: UUID)
}
