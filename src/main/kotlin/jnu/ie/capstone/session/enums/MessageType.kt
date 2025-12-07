package jnu.ie.capstone.session.enums

enum class MessageType {
    SERVER_READY,
    UPDATE_SHOPPING_CART,
    OUTPUT_TEXT_CHUNK,
    OUTPUT_TEXT_RESULT,
    CHANGE_STATE,

    PROCESS_PAYMENT;
}