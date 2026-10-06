package app.cloudfit.accounts.infrastructure.adapter.output.email

import app.cloudfit.accounts.application.port.output.EmailMessage
import app.cloudfit.accounts.application.port.output.EmailSender
import org.slf4j.LoggerFactory

class LogEmailSender : EmailSender {
    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun send(message: EmailMessage) {
        log.info("[DEV EMAIL] to={} subject=\"{}\"\n{}", message.to, message.subject, message.text)
    }
}
