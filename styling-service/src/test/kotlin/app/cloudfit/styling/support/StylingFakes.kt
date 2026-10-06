package app.cloudfit.styling.support

import app.cloudfit.shared.application.error.InsufficientCreditsException
import app.cloudfit.styling.application.port.output.CreditWallet
import app.cloudfit.styling.application.port.output.LookRepository
import app.cloudfit.styling.domain.Look
import app.cloudfit.styling.domain.LookStatus
import java.time.Instant
import java.util.UUID

class InMemoryLookRepository(private val now: () -> Instant) : LookRepository {
    val looks = linkedMapOf<UUID, Look>()
    val updatedAt = mutableMapOf<UUID, Instant>()

    override suspend fun create(look: Look) {
        looks[look.id] = look
        updatedAt[look.id] = now()
    }

    override suspend fun findById(id: UUID): Look? = looks[id]

    override suspend fun find(userId: UUID, id: UUID): Look? = looks[id]?.takeIf { it.userId == userId }

    override suspend fun list(userId: UUID, savedOnly: Boolean, limit: Int): List<Look> =
        looks.values.filter { it.userId == userId && (!savedOnly || it.saved) }.sortedByDescending { it.createdAt }.take(limit)

    override suspend fun updateProgress(look: Look): Boolean {
        val current = looks[look.id] ?: return false
        if (!current.status.inProgress) return false
        looks[look.id] = look.copy(saved = current.saved)
        updatedAt[look.id] = now()
        return true
    }

    override suspend fun failIfStale(id: UUID, failureCode: String, updatedBefore: Instant): Boolean {
        val current = looks[id] ?: return false
        if (!current.status.inProgress || !updatedAt.getValue(id).isBefore(updatedBefore)) return false
        looks[id] = current.copy(status = LookStatus.FAILED, failureCode = failureCode)
        return true
    }

    override suspend fun findStale(updatedBefore: Instant): List<Look> =
        looks.values.filter { it.status.inProgress && updatedAt.getValue(it.id).isBefore(updatedBefore) }

    override suspend fun markSaved(userId: UUID, id: UUID) {
        find(userId, id)?.let { looks[id] = it.copy(saved = true) }
    }

    override suspend fun delete(userId: UUID, id: UUID): Boolean = find(userId, id)?.let { looks.remove(id) } != null
}

class FakeCreditWallet(var credits: Int = 1) : CreditWallet {
    val reserved = mutableListOf<UUID>()
    val confirmed = mutableListOf<UUID>()
    val refunded = mutableListOf<UUID>()

    override suspend fun reserve(userId: UUID, lookId: UUID) {
        if (credits <= 0) throw InsufficientCreditsException()
        credits--
        reserved += lookId
    }

    override suspend fun confirm(lookId: UUID) {
        confirmed += lookId
    }

    override suspend fun refund(lookId: UUID) {
        if (lookId in reserved && lookId !in refunded && lookId !in confirmed) {
            credits++
            refunded += lookId
        }
    }
}
