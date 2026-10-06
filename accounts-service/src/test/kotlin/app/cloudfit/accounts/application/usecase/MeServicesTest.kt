package app.cloudfit.accounts.application.usecase

import app.cloudfit.accounts.application.port.input.RegisterCommand
import app.cloudfit.accounts.application.port.input.UpdateMeCommand
import app.cloudfit.accounts.application.port.output.AccountDataEraser
import app.cloudfit.accounts.support.AccountsTestKit
import app.cloudfit.shared.application.error.UnauthorizedException
import app.cloudfit.shared.application.error.ValidationException
import app.cloudfit.shared.domain.AppLocale
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

class MeServicesTest : StringSpec({

    suspend fun AccountsTestKit.verifiedUser(): UUID {
        register.execute(RegisterCommand("ana@example.com", "supersecret1", "en", null))
        return verifyEmail.execute(emails.lastToken()).me.id
    }

    "get and update me" {
        val kit = AccountsTestKit()
        val id = kit.verifiedUser()

        GetMeService(kit.users, kit.meAssembler).execute(id).credits.balance shouldBe 5
        val updated = UpdateMeService(kit.users, kit.meAssembler).execute(UpdateMeCommand(id, "  Ana  ", "es"))

        updated.displayName shouldBe "Ana"
        updated.locale shouldBe AppLocale.ES
        updated.adsEnabled shouldBe true
        updated.maxClothes shouldBe 30
        shouldThrow<ValidationException> { UpdateMeService(kit.users, kit.meAssembler).execute(UpdateMeCommand(id, null, "de")) }
    }

    "delete account runs every eraser even if one fails, then removes the user" {
        val kit = AccountsTestKit()
        val id = kit.verifiedUser()
        val erased = mutableListOf<String>()

        DeleteAccountService(
            kit.users,
            listOf(
                AccountDataEraser { error("stripe down") },
                AccountDataEraser { erased += "images:$it" },
            ),
        ).execute(id)

        erased shouldBe listOf("images:$id")
        kit.users.users.containsKey(id) shouldBe false
        shouldThrow<UnauthorizedException> { GetMeService(kit.users, kit.meAssembler).execute(id) }
        GetAccountProfileService(kit.users).execute(id) shouldBe null
    }
})
