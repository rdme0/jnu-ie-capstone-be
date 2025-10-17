package jnu.ie.capstone.session.enums

enum class SessionState {
    MENU_SELECTION,
    CART_CONFIRMATION,
    PAYMENT_CONFIRMATION,
    COMPLETED,
    CANCELLED;

    val text: String
        get() = this.name
}