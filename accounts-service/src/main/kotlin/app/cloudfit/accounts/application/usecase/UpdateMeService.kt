package app.cloudfit.accounts.application.usecase

import app.cloudfit.accounts.application.port.input.UpdateMeCommand
import app.cloudfit.accounts.application.port.input.UpdateMeUseCase
import app.cloudfit.accounts.application.port.output.UserRepository
import app.cloudfit.accounts.domain.Me
import app.cloudfit.shared.application.error.UnauthorizedException

class UpdateMeService(
    private val users: UserRepository,
    private val meAssembler: MeAssembler,
) : UpdateMeUseCase {
    override suspend fun execute(command: UpdateMeCommand): Me {
        val user = users.findById(command.userId) ?: throw UnauthorizedException("Account no longer exists")
        val updated = user.copy(
            displayName = if (command.displayName != null) AccountValidation.displayName(command.displayName) else user.displayName,
            locale = AccountValidation.locale(command.locale, default = user.locale),
        )
        if (updated != user) users.update(updated)
        return meAssembler.assemble(updated)
    }
}
