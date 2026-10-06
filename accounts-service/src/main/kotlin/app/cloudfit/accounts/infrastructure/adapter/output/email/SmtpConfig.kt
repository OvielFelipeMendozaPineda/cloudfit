package app.cloudfit.accounts.infrastructure.adapter.output.email

data class SmtpConfig(
    val host: String,
    val port: Int,
    val user: String,
    val password: String,
    val startTls: Boolean,
    val from: String,
) {
    val configured: Boolean get() = host.isNotBlank()
}
