package jnu.ie.capstone.session.handler

import jnu.ie.capstone.common.security.dto.KioskUserDetails
import jnu.ie.capstone.session.enums.SessionEvent
import jnu.ie.capstone.session.enums.SessionState
import jnu.ie.capstone.session.service.KioskAiSessionService
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import mu.KotlinLogging
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.statemachine.StateMachine
import org.springframework.statemachine.config.StateMachineFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.BinaryMessage
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.BinaryWebSocketHandler
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

private val logger = KotlinLogging.logger {}

@Component
class KioskAiSessionHandler(
    private val kioskAiSessionService: KioskAiSessionService,
    private val stateMachineFactory: StateMachineFactory<SessionState, SessionEvent>
) : BinaryWebSocketHandler() {

    private val sessions = ConcurrentHashMap<String, StateMachine<SessionState, SessionEvent>>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        logger.info { "연결 성공 -> ${session.id}" }

        val stateMachine = stateMachineFactory.getStateMachine(session.id)

        stateMachine.startReactively().subscribe()

        sessions[session.id] = stateMachine
        logger.info { "${session.id} statemachine 생성 완료. 현재 상태 -> ${stateMachine.state.id}" }

        val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)


        val clientVoiceStream = MutableSharedFlow<ByteArray>(
            extraBufferCapacity = 128,
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
                    stateMachine
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