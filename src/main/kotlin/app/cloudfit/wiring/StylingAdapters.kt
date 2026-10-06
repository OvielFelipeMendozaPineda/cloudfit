package app.cloudfit.wiring

import app.cloudfit.accounts.application.port.input.GetAccountProfileUseCase
import app.cloudfit.billing.application.port.input.ConfirmCreditsUseCase
import app.cloudfit.billing.application.port.input.RefundCreditsUseCase
import app.cloudfit.billing.application.port.input.ReserveCreditsUseCase
import app.cloudfit.shared.domain.AppLocale
import app.cloudfit.styling.application.port.output.CreditWallet
import app.cloudfit.styling.application.port.output.RenderedImageSaver
import app.cloudfit.styling.application.port.output.UserLocaleReader
import app.cloudfit.styling.application.port.output.WardrobeReader
import app.cloudfit.styling.domain.WardrobeItem
import app.cloudfit.styling.domain.WardrobeSnapshot
import app.cloudfit.wardrobe.application.port.input.GetAvatarUseCase
import app.cloudfit.wardrobe.application.port.input.ListClothesUseCase
import app.cloudfit.wardrobe.application.port.input.UploadImageUseCase
import java.util.UUID

class WardrobeReaderAdapter(
    private val listClothes: ListClothesUseCase,
    private val getAvatar: GetAvatarUseCase,
) : WardrobeReader {
    override suspend fun snapshot(userId: UUID): WardrobeSnapshot = WardrobeSnapshot(
        items = listClothes.execute(userId).map {
            WardrobeItem(
                id = it.id,
                category = it.category,
                imageUrl = it.imageUrl,
                name = it.name,
                colour = it.colour,
                pattern = it.pattern,
                formality = it.formality,
                warmth = it.warmth,
                description = it.description,
            )
        },
        avatarUrl = getAvatar.execute(userId),
    )
}

class CreditWalletAdapter(
    private val reserveCredits: ReserveCreditsUseCase,
    private val confirmCredits: ConfirmCreditsUseCase,
    private val refundCredits: RefundCreditsUseCase,
) : CreditWallet {
    override suspend fun reserve(userId: UUID, lookId: UUID) = reserveCredits.execute(userId, lookRef(lookId))

    override suspend fun confirm(lookId: UUID) = confirmCredits.execute(lookRef(lookId))

    override suspend fun refund(lookId: UUID) {
        refundCredits.execute(lookRef(lookId))
    }

    private fun lookRef(lookId: UUID) = "look:$lookId"
}

class UserLocaleAdapter(private val profiles: GetAccountProfileUseCase) : UserLocaleReader {
    override suspend fun locale(userId: UUID): AppLocale = profiles.execute(userId)?.locale ?: AppLocale.EN
}

class RenderedImageSaverAdapter(private val upload: UploadImageUseCase) : RenderedImageSaver {
    override suspend fun save(userId: UUID, bytes: ByteArray, contentType: String): String =
        upload.execute(userId, bytes, contentType)
}
