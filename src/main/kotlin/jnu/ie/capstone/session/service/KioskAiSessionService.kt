package jnu.ie.capstone.session.service

import jnu.ie.capstone.rtzr.service.RtzrSttService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class KioskAiSessionService(
    private val sttService: RtzrSttService
) {

    suspend fun processVoiceChunk(voiceStream: Flow<ByteArray>, scope: CoroutineScope) {
        val result = sttService.stt(voiceStream, scope)
            .collect { result -> logger.info { "변환된 chunk dto -> $result" } }
    }

}