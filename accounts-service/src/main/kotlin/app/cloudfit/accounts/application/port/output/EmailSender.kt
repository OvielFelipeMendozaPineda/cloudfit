package app.cloudfit.accounts.application.port.output

data class EmailMessage(
    val to: String,
    val subject: String,
    val text: String,
    val html: String,
)

fun interface EmailSender {
    suspend fun send(message: EmailMessage)
}
