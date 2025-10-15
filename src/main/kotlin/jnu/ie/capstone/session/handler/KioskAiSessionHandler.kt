package jnu.ie.capstone.session.handler

import jnu.ie.capstone.common.security.dto.KioskUserDetails
import jnu.ie.capstone.session.service.KioskAiSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.socket.BinaryMessage
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.BinaryWebSocketHandler
import kotlin.coroutines.cancellation.CancellationException

private val logger = KotlinLogging.logger {}

@Component
class KioskAiSessionHandler(
    private val kioskAiSessionService: KioskAiSessionService
) : BinaryWebSocketHandler() {

    override fun afterConnectionEstablished(session: WebSocketSession) {
        logger.info { "Connection established: ${session.id}" }

        val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        val clientVoiceStream = MutableSharedFlow<ByteArray>(
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

        val authentication = session.attributes["principal"] as? UsernamePasswordAuthenticationToken

        val userDetails = authentication?.principal as? KioskUserDetails
            ?: run {
                logger.error { "올바르지 않은 authentication -> ${session.attributes["principal"]}" }
                session.close(CloseStatus.POLICY_VIOLATION)
                return
            }

        val storeId = session.attributes["storeId"] as? Long
            ?: run {
                logger.error { "올바르지 않은 storeId -> ${session.attributes["storeId"]}" }
                session.close(CloseStatus.BAD_DATA)
                return
            }

        sessionScope.launch {
            try {
                kioskAiSessionService.processVoiceChunk(
                    clientVoiceStream,
                    storeId,
                    userDetails.memberInfo,
                    session
                )
            } catch (_: CancellationException) {
                logger.info { "세션 ${session.id} 처리가 정상적으로 취소되었습니다." }
            } catch (e: Exception) {
                logger.error(e) { "voice chunk 처리 중 에러 -> ${session.id}" }
            }
        }

        session.attributes["clientVoiceStream"] = clientVoiceStream
        session.attributes["sessionScope"] = sessionScope
    }

    override fun handleBinaryMessage(session: WebSocketSession, message: BinaryMessage) {
        val clientVoiceStream =
            session.attributes["clientVoiceStream"] as? MutableSharedFlow<ByteArray>

        val bytes = ByteArray(message.payload.remaining())
        message.payload.get(bytes)

        (session.attributes["sessionScope"] as? CoroutineScope)?.launch {
            clientVoiceStream?.emit(bytes)
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        logger.info { "Client disconnected: ${session.id}" }

        (session.attributes["sessionScope"] as? CoroutineScope)?.cancel()
    }

}