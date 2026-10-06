package app.cloudfit.shared.domain

enum class AppLocale(val code: String) {
    EN("en"),
    ES("es"),
    ;

    companion object {
        fun parse(code: String?): AppLocale? = entries.firstOrNull { it.code == code?.trim()?.lowercase() }
    }
}
