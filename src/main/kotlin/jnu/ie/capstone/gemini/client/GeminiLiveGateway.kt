package jnu.ie.capstone.gemini.client

import jnu.ie.capstone.gemini.constant.enums.GeminiModel
import jnu.ie.capstone.gemini.dto.client.request.GeminiInput
import jnu.ie.capstone.gemini.dto.client.response.GeminiOutput
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow

interface GeminiLiveGateway {
    suspend fun getLiveResponse(
        geminiReadySignal: CompletableDeferred<Unit>,
        inputData: Flow<GeminiInput>,
        prompt: String,
        model: GeminiModel = GeminiModel.GEMINI_2_5_FLASH_NATIVE_AUDIO
    ): Flow<GeminiOutput>
}
