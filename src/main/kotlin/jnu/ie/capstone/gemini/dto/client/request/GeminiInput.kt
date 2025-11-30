package jnu.ie.capstone.gemini.dto.client.request

import jnu.ie.capstone.gemini.dto.client.internal.Context

sealed class GeminiInput {
    @Suppress("ArrayInDataClass")
    data class Audio(val chunk: ByteArray) : GeminiInput()
    data class Text(val context: Context) : GeminiInput()

    data class ToolResponse(
        val id: String?,
        val functionName: String,
        val result: String
    ) : GeminiInput()
}