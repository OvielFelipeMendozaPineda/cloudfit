package app.cloudfit.styling.application.port.output

import app.cloudfit.styling.domain.Look
import java.time.Instant
import java.util.UUID

interface LookRepository {
    suspend fun create(look: Look)

    suspend fun findById(id: UUID): Look?

    suspend fun find(userId: UUID, id: UUID): Look?

    suspend fun list(userId: UUID, savedOnly: Boolean, limit: Int): List<Look>

    suspend fun updateProgress(look: Look): Boolean

    suspend fun failIfStale(id: UUID, failureCode: String, updatedBefore: Instant): Boolean

    suspend fun findStale(updatedBefore: Instant): List<Look>

    suspend fun markSaved(userId: UUID, id: UUID)

    suspend fun delete(userId: UUID, id: UUID): Boolean
}
