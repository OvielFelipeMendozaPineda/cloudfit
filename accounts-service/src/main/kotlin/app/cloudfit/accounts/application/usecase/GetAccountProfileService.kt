package app.cloudfit.accounts.application.usecase

import app.cloudfit.accounts.application.port.input.GetAccountProfileUseCase
import app.cloudfit.accounts.application.port.output.UserRepository
import app.cloudfit.accounts.domain.AccountProfile
import java.util.UUID

class GetAccountProfileService(private val users: UserRepository) : GetAccountProfileUseCase {
    override suspend fun execute(userId: UUID): AccountProfile? =
        users.findById(userId)?.let { AccountProfile(email = it.email, locale = it.locale) }
}
