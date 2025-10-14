package jnu.ie.capstone.gemini.dto.client.request

import jnu.ie.capstone.gemini.dto.client.internal.Context

sealed class GeminiInput {
    data class Audio(val chunk: ByteArray) : GeminiInput()
    data class Text(val context: Context) : GeminiInput()
}