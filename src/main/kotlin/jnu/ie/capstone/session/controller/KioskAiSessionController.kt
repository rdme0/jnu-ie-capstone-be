package jnu.ie.capstone.session.controller

import jnu.ie.capstone.session.dto.request.VoiceRequestChunk
import jnu.ie.capstone.session.service.KioskAiSessionService
import kotlinx.coroutines.flow.Flow
import mu.KotlinLogging
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller

private val logger = KotlinLogging.logger {}

@Controller
class KioskAiSessionController(
    private val service: KioskAiSessionService
) {

    @MessageMapping("order.voice")
    suspend fun receiveOrderVoice(voiceStream: Flow<VoiceRequestChunk>) {
        logger.info { "order voice 요청을 받았습니다." }
        service.processVoiceChunk(voiceStream)
    }

}