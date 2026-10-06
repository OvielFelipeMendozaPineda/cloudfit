package app.cloudfit.styling.application.port.input

import app.cloudfit.styling.domain.Look
import java.util.UUID

interface CreateLookUseCase {
    suspend fun execute(userId: UUID, event: String): Look
}

interface GetLookUseCase {
    suspend fun execute(userId: UUID, id: UUID): Look
}

interface ListLooksUseCase {
    suspend fun execute(userId: UUID, savedOnly: Boolean): List<Look>
}

interface SaveLookUseCase {
    suspend fun execute(userId: UUID, id: UUID): Look
}

interface DeleteLookUseCase {
    suspend fun execute(userId: UUID, id: UUID)
}

interface ProcessLookUseCase {
    suspend fun execute(lookId: UUID)
}

interface RecoverStuckLooksUseCase {
    suspend fun execute(): Int
}
