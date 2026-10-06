package app.cloudfit.accounts.infrastructure.adapter.output.email

import app.cloudfit.accounts.application.port.output.EmailMessage
import app.cloudfit.accounts.application.port.output.EmailSender
import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import java.util.Properties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class SmtpEmailSender(
    private val config: SmtpConfig,
    private val scope: CoroutineScope,
) : EmailSender {
    private val log = LoggerFactory.getLogger(javaClass)

    private val session: Session = Session.getInstance(
        Properties().apply {
            put("mail.smtp.host", config.host)
            put("mail.smtp.port", config.port.toString())
            put("mail.smtp.auth", config.user.isNotBlank().toString())
            put("mail.smtp.starttls.enable", config.startTls.toString())
            put("mail.smtp.connectiontimeout", "10000")
            put("mail.smtp.timeout", "10000")
        },
        if (config.user.isNotBlank()) {
            object : Authenticator() {
                override fun getPasswordAuthentication() = PasswordAuthentication(config.user, config.password)
            }
        } else {
            null
        },
    )

    override suspend fun send(message: EmailMessage) {
        scope.launch(Dispatchers.IO) {
            runCatching { Transport.send(toMime(message)) }
                .onFailure { log.error("SMTP delivery to {} failed: {}", message.to, it.message) }
        }
    }

    private fun toMime(message: EmailMessage) = MimeMessage(session).apply {
        setFrom(InternetAddress(config.from))
        setRecipients(Message.RecipientType.TO, InternetAddress.parse(message.to))
        setSubject(message.subject, "UTF-8")
        setContent(
            MimeMultipart("alternative").apply {
                addBodyPart(MimeBodyPart().apply { setText(message.text, "UTF-8") })
                addBodyPart(MimeBodyPart().apply { setContent(message.html, "text/html; charset=UTF-8") })
            },
        )
    }
}
