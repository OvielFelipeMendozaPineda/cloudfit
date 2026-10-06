package app.cloudfit.styling.application.usecase

import app.cloudfit.shared.application.error.NotFoundException
import app.cloudfit.styling.application.port.input.DeleteLookUseCase
import app.cloudfit.styling.application.port.output.CreditWallet
import app.cloudfit.styling.application.port.output.LookRepository
import java.util.UUID

class DeleteLookService(
    private val looks: LookRepository,
    private val wallet: CreditWallet,
) : DeleteLookUseCase {
    override suspend fun execute(userId: UUID, id: UUID) {
        val look = looks.find(userId, id) ?: throw NotFoundException("Look $id not found")
        if (!looks.delete(userId, id)) throw NotFoundException("Look $id not found")
        if (look.status.inProgress) wallet.refund(look.id)
    }
}
