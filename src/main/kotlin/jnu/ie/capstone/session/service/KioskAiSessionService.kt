package jnu.ie.capstone.session.service

import jnu.ie.capstone.clova.service.ClovaSpeechService
import jnu.ie.capstone.session.dto.request.VoiceRequestChunk
import kotlinx.coroutines.flow.Flow
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class KioskAiSessionService(
    private val speechService: ClovaSpeechService
) {
    suspend fun processVoiceChunk(voiceStream: Flow<VoiceRequestChunk>) {
        val result = speechService.recognizeVoice(voiceStream) {
            text -> logger.info { "변환된 text 조각 : $text" }
        }
    }

}