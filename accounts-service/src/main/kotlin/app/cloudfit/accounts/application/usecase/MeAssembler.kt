package app.cloudfit.accounts.application.usecase

import app.cloudfit.accounts.application.port.output.AccountBalanceReader
import app.cloudfit.accounts.application.port.output.AvatarUrlReader
import app.cloudfit.accounts.application.port.output.IdentityRepository
import app.cloudfit.accounts.application.port.output.WardrobeLimitsReader
import app.cloudfit.accounts.domain.AuthProvider
import app.cloudfit.accounts.domain.Me
import app.cloudfit.accounts.domain.User

class MeAssembler(
    private val identities: IdentityRepository,
    private val balances: AccountBalanceReader,
    private val avatars: AvatarUrlReader,
    private val limits: WardrobeLimitsReader,
) {
    suspend fun assemble(user: User): Me {
        val providers = buildList {
            if (user.passwordHash != null) add(AuthProvider.PASSWORD)
            addAll(identities.listProviders(user.id))
        }.distinct()
        val balance = balances.read(user.id)
        return Me(
            id = user.id,
            email = user.email,
            emailVerified = user.emailVerified,
            displayName = user.displayName,
            locale = user.locale,
            avatarUrl = avatars.avatarUrl(user.id),
            providers = providers,
            credits = balance.credits,
            plan = balance.plan,
            adsEnabled = balance.plan == null,
            maxClothes = limits.maxClothes(user.id),
        )
    }
}
