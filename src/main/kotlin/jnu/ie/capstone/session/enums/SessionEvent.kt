package jnu.ie.capstone.session.enums


enum class SessionEvent {
    CONFIRM_PAYMENT,
    PROCESS_PAYMENT,
    PREVIOUS,
    CANCEL;

    val text: String
        get() = this.name

    companion object {
        fun fromText(text: String): SessionEvent? {
            return entries.find { it.text == text }
        }
    }
}