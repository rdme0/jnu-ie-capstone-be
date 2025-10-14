package jnu.ie.capstone.gemini.dto.client.response

import jnu.ie.capstone.session.enums.SessionEvent

sealed class GeminiOutput {
    data class InputSTT(val text: String) : GeminiOutput()
    data class OutputSTT(val text: String) : GeminiOutput()
    data class OutputFunction(val event: SessionEvent) : GeminiOutput()
    data class OutputVoiceStream(val chunk: ByteArray) : GeminiOutput()
    data class EndOfGeminiTurn(
        val finalInputSTT: String, val finalOutputSTT: String
    ) : GeminiOutput()
}