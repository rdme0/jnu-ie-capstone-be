package jnu.ie.capstone.session.enums

enum class SessionEvent(val text: String) {
    CONFIRM_CART("CONFIRM_CART"),
    CONFIRM_PAYMENT("CONFIRM_PAYMENT"),
    PROCESS_PAYMENT("PROCESS_PAYMENT"),
    PREVIOUS("PREVIOUS"),
    CANCEL("CANCEL");

    companion object {
        fun fromText(text: String?): SessionEvent? {
            text ?: return null
            return entries.find { it.text == text }
        }
    }
}