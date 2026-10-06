package app.cloudfit.accounts.infrastructure.config

import app.cloudfit.accounts.domain.AccountsPolicy
import app.cloudfit.accounts.infrastructure.adapter.output.email.SmtpConfig
import io.ktor.server.config.ApplicationConfig

data class AccountsConfig(
    val policy: AccountsPolicy,
    val googleClientId: String,
    val appleClientId: String,
    val cookieSecure: Boolean,
    val smtp: SmtpConfig,
) {
    companion object {
        fun from(config: ApplicationConfig, appUrl: String): AccountsConfig {
            val auth = config.config("cloudfit.auth")
            val mail = config.config("cloudfit.mail")
            return AccountsConfig(
                policy = AccountsPolicy(
                    appUrl = appUrl,
                    appleRedirectUri = auth.propertyOrNull("appleRedirectUri")?.getString()?.ifBlank { null },
                ),
                googleClientId = auth.property("googleClientId").getString(),
                appleClientId = auth.property("appleClientId").getString(),
                cookieSecure = auth.property("cookieSecure").getString().toBooleanStrictOrNull() ?: true,
                smtp = SmtpConfig(
                    host = mail.property("smtpHost").getString(),
                    port = mail.property("smtpPort").getString().toIntOrNull() ?: 587,
                    user = mail.property("smtpUser").getString(),
                    password = mail.property("smtpPassword").getString(),
                    startTls = mail.property("smtpStartTls").getString().toBooleanStrictOrNull() ?: true,
                    from = mail.property("from").getString(),
                ),
            )
        }
    }
}
