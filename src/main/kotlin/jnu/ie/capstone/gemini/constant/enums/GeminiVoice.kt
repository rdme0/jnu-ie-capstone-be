package jnu.ie.capstone.gemini.constant.enums

enum class GeminiVoice(val text: String) {

    ZEPHYR("Zephyr"),
    PUCK("Puck"),
    CHARON("Charon"),
    KORE("Kore"),
    FENRIR("Fenrir");

    override fun toString(): String {
        return text
    }
}