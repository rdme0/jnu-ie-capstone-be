package jnu.ie.capstone.gemini.constant.enums

enum class GeminiModel(val text: String) {
    GEMINI_EMBEDDING_001("gemini-embedding-001"),
    GEMINI_3_8_LIVE("gemini-3.8-live");

    override fun toString(): String {
        return text
    }
}