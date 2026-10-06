package app.cloudfit.accounts.application.port.output

import app.cloudfit.accounts.domain.AccessToken
import java.util.UUID

fun interface TokenIssuer {
    fun issueAccessToken(userId: UUID): AccessToken
}
