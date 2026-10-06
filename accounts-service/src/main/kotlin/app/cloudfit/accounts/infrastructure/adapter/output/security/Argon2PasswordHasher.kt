package app.cloudfit.accounts.infrastructure.adapter.output.security

import app.cloudfit.accounts.application.port.output.PasswordHasher
import com.password4j.Argon2Function
import com.password4j.Password
import com.password4j.types.Argon2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Argon2PasswordHasher(
    memoryKib: Int = 19_456,
    iterations: Int = 2,
    parallelism: Int = 1,
) : PasswordHasher {
    private val function = Argon2Function.getInstance(memoryKib, iterations, parallelism, 32, Argon2.ID, 19)

    override suspend fun hash(password: String): String = withContext(Dispatchers.Default) {
        Password.hash(password).addRandomSalt(16).with(function).result
    }

    override suspend fun verify(password: String, hash: String): Boolean = withContext(Dispatchers.Default) {
        runCatching { Password.check(password, hash).with(Argon2Function.getInstanceFromHash(hash)) }.getOrDefault(false)
    }
}
