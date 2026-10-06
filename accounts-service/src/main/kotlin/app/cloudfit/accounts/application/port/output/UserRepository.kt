package app.cloudfit.accounts.application.port.output

import app.cloudfit.accounts.domain.User
import java.util.UUID

interface UserRepository {
    suspend fun findById(id: UUID): User?

    suspend fun findByEmail(email: String): User?

    suspend fun create(user: User)

    suspend fun update(user: User)

    suspend fun delete(id: UUID)
}
