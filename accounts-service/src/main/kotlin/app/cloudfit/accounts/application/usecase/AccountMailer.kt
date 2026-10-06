package app.cloudfit.accounts.application.usecase

import app.cloudfit.accounts.application.port.output.EmailMessage
import app.cloudfit.accounts.application.port.output.EmailSender
import app.cloudfit.shared.domain.AppLocale
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class AccountMailer(
    private val sender: EmailSender,
    private val appUrl: String,
) {
    suspend fun sendVerification(to: String, locale: AppLocale, token: String) {
        val link = "$appUrl/verify-email?token=${encode(token)}"
        send(
            to = to,
            subject = pick(locale, "Verify your CloudFit email", "Verifica tu correo de CloudFit"),
            body = pick(
                locale,
                "Welcome to CloudFit! Confirm your email to start styling your wardrobe:",
                "¡Bienvenido a CloudFit! Confirma tu correo para empezar a armar tus looks:",
            ),
            link = link,
            footer = pick(locale, "This link expires in 24 hours.", "Este enlace vence en 24 horas."),
        )
    }

    suspend fun sendAlreadyRegistered(to: String, locale: AppLocale) {
        send(
            to = to,
            subject = pick(locale, "You already have a CloudFit account", "Ya tienes una cuenta en CloudFit"),
            body = pick(
                locale,
                "Someone tried to sign up with this email, but you already have an account. Log in or reset your password:",
                "Alguien intentó registrarse con este correo, pero ya tienes una cuenta. Inicia sesión o restablece tu contraseña:",
            ),
            link = "$appUrl/login",
            footer = pick(locale, "If it wasn't you, you can ignore this email.", "Si no fuiste tú, ignora este correo."),
        )
    }

    suspend fun sendPasswordReset(to: String, locale: AppLocale, token: String) {
        send(
            to = to,
            subject = pick(locale, "Reset your CloudFit password", "Restablece tu contraseña de CloudFit"),
            body = pick(locale, "Use this link to choose a new password:", "Usa este enlace para elegir una nueva contraseña:"),
            link = "$appUrl/reset-password?token=${encode(token)}",
            footer = pick(
                locale,
                "This link expires in 1 hour. If you didn't ask for it, ignore this email.",
                "Este enlace vence en 1 hora. Si no lo pediste, ignora este correo.",
            ),
        )
    }

    private suspend fun send(to: String, subject: String, body: String, link: String, footer: String) {
        sender.send(
            EmailMessage(
                to = to,
                subject = subject,
                text = "$body\n\n$link\n\n$footer\n",
                html = """
                    <p>${escape(body)}</p>
                    <p><a href="${escape(link)}">${escape(link)}</a></p>
                    <p style="color:#666;font-size:12px">${escape(footer)}</p>
                """.trimIndent(),
            ),
        )
    }

    private fun pick(locale: AppLocale, en: String, es: String) = if (locale == AppLocale.ES) es else en

    private fun encode(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8)

    private fun escape(value: String) = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
