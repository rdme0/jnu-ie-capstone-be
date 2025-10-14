package jnu.ie.capstone.session.enums

enum class SessionState(val text : String) {
    MENU_SELECTION("MENU_SELECTION"),
    CART_CONFIRMATION("CART_CONFIRMATION"),
    PAYMENT_CONFIRMATION("PAYMENT_CONFIRMATION"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED");

    override fun toString(): String {
        return text
    }
}