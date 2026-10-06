package app.cloudfit.accounts.application.usecase

import app.cloudfit.accounts.application.port.input.GetMeUseCase
import app.cloudfit.accounts.application.port.output.UserRepository
import app.cloudfit.accounts.domain.Me
import app.cloudfit.shared.application.error.UnauthorizedException
import java.util.UUID

class GetMeService(
    private val users: UserRepository,
    private val meAssembler: MeAssembler,
) : GetMeUseCase {
    override suspend fun execute(userId: UUID): Me {
        val user = users.findById(userId) ?: throw UnauthorizedException("Account no longer exists")
        return meAssembler.assemble(user)
    }
}
