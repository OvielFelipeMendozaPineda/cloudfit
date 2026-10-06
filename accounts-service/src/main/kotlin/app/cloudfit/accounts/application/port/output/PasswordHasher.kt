package app.cloudfit.accounts.application.port.output

interface PasswordHasher {
    suspend fun hash(password: String): String

    suspend fun verify(password: String, hash: String): Boolean
}
