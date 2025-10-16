package jnu.ie.capstone.gemini.dto.client.response

import jnu.ie.capstone.gemini.constant.enums.GeminiFunctionSignature

sealed class GeminiOutput {
    data class InputSTT(val text: String) : GeminiOutput()
    data class OutputSTT(val text: String) : GeminiOutput()

    data class FunctionCall(
        val name: GeminiFunctionSignature,
        val params: GeminiFunctionParams
    ) : GeminiOutput()

    data class VoiceStream(val chunk: ByteArray) : GeminiOutput()

    data class EndOfGeminiTurn(
        val finalInputSTT: String,
        val finalOutputSTT: String
    ) : GeminiOutput()
}