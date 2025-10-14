package jnu.ie.capstone.session.service

import jnu.ie.capstone.gemini.client.GeminiLiveClient
import jnu.ie.capstone.gemini.dto.client.internal.Context
import jnu.ie.capstone.gemini.dto.client.request.GeminiInput
import jnu.ie.capstone.gemini.dto.client.response.GeminiOutput.*
import jnu.ie.capstone.member.dto.MemberInfo
import jnu.ie.capstone.menu.service.MenuCoordinateService
import jnu.ie.capstone.rtzr.service.RtzrSttService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.web.socket.WebSocketSession

@Service
class KioskAiSessionService(
    private val liveClient: GeminiLiveClient,
    private val menuService: MenuCoordinateService,
    private val sttService: RtzrSttService
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val scopeForContext = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun processVoiceChunk(
        voiceStream: Flow<ByteArray>,
        storeId: Long,
        ownerInfo: MemberInfo,
        session: WebSocketSession
    ) {
        val sharedVoiceStream = voiceStream.shareIn(scopeForContext, SharingStarted.Lazily)

        val voiceFastInput: Flow<GeminiInput.Audio> = sharedVoiceStream.map {
            GeminiInput.Audio(it)
        }

        val contextSlowInput: Flow<GeminiInput.Text> = sttService.stt(sharedVoiceStream, scopeForContext)
            .filter { it.final }
            .map { it.alternatives.first().text }
            .map { menuService.getRelevant(text = it, storeId, ownerInfo) }
            .map { GeminiInput.Text(Context.MenuContext(menus = it)) }

        val mergedInput = merge(voiceFastInput, contextSlowInput)
            .onEach { logger.debug { "gemini input -> $it"} }

        liveClient.getLiveResponse(mergedInput, "")
            .collect { output ->
                logger.debug { "output -> $output" }
                when (output) {
                    is InputSTT -> {

                    }

                    is OutputSTT -> {

                    }

                    is OutputFunction -> {

                    }

                    is OutputVoiceStream -> {

                    }

                    is EndOfGeminiTurn -> {
                    }
                }
            }
    }

}