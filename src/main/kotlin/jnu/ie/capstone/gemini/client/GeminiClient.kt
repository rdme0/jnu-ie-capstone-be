package jnu.ie.capstone.gemini.client

import com.google.genai.Client
import com.google.genai.types.LiveConnectConfig
import com.google.genai.types.ProactivityConfig
import jnu.ie.capstone.gemini.config.GeminiConfig
import org.springframework.stereotype.Component

@Component
class GeminiClient(
    private val config: GeminiConfig
) {
    private val liveConfig = LiveConnectConfig.builder()
        .responseModalities("AUDIO", "TEXT")
        .proactivity(ProactivityConfig.builder().proactiveAudio(true))
        .build()

    private val client = Client.builder().apiKey(config.apiKey).build()

    suspend fun getLiveResponse() {
        client.async.live.connect(
            "gemini-2.5-flash-preview-native-audio-dialog",
            liveConfig
        )
    }

}