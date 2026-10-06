package app.cloudfit.accounts.application.usecase

import app.cloudfit.shared.application.error.ValidationException
import app.cloudfit.shared.domain.AppLocale

internal object AccountValidation {
    private val emailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    private const val MIN_PASSWORD = 8
    private const val MAX_PASSWORD = 128
    private const val MAX_EMAIL = 254
    private const val MAX_DISPLAY_NAME = 80

    fun email(raw: String): String {
        val email = raw.trim().lowercase()
        if (email.length > MAX_EMAIL || !emailPattern.matches(email)) throw ValidationException("Invalid email")
        return email
    }

    fun password(raw: String): String {
        if (raw.length !in MIN_PASSWORD..MAX_PASSWORD) {
            throw ValidationException("Password must be $MIN_PASSWORD-$MAX_PASSWORD characters")
        }
        return raw
    }

    fun locale(raw: String?, default: AppLocale = AppLocale.EN): AppLocale {
        if (raw == null) return default
        return AppLocale.parse(raw) ?: throw ValidationException("locale must be 'en' or 'es'")
    }

    fun displayName(raw: String?): String? {
        val name = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (name.length > MAX_DISPLAY_NAME) throw ValidationException("displayName max $MAX_DISPLAY_NAME characters")
        return name
    }
}
