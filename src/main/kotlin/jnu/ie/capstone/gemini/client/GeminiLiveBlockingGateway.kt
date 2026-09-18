package jnu.ie.capstone.gemini.client

import jnu.ie.capstone.gemini.constant.enums.GeminiModel
import jnu.ie.capstone.gemini.dto.client.request.GeminiInput
import jnu.ie.capstone.gemini.dto.client.response.GeminiOutput

interface GeminiLiveBlockingGateway {
    fun connect(
        prompt: String,
        model: GeminiModel = GeminiModel.GEMINI_3_8_LIVE
    ): GeminiLiveBlockingSessionGateway
}

interface GeminiLiveBlockingSessionGateway : AutoCloseable {
    fun send(input: GeminiInput)

    fun takeOutput(): GeminiOutput
}
