package jnu.ie.capstone.session.handler

import jnu.ie.capstone.common.security.dto.KioskUserDetails
import jnu.ie.capstone.common.websocket.util.WebSocketReplier
import jnu.ie.capstone.session.dto.internal.ShoppingCartDTO
import jnu.ie.capstone.session.enums.SessionEvent
import jnu.ie.capstone.session.enums.SessionState
import jnu.ie.capstone.session.event.ServerReadyEvent
import jnu.ie.capstone.session.registry.WebSocketSessionRegistry
import jnu.ie.capstone.session.service.KioskSessionService
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import mu.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
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

@Component
class KioskAiSessionHandler(
    private val kioskSessionService: KioskSessionService,
    private val stateMachineFactory: StateMachineFactory<SessionState, SessionEvent>,
    private val sessionRegistry: WebSocketSessionRegistry,
    private val eventPublisher: ApplicationEventPublisher
) : BinaryWebSocketHandler() {

    private companion object {
        const val SESSION_SCOPE_KEY = "sessionScope"
        const val CLIENT_VOICE_STREAM_KEY = "clientVoiceStream"
        const val SHOPPING_CART_KEY = "shoppingCart"
        const val REPLIER_KEY = "replier"
        const val PRINCIPAL_KEY = "principal"
        const val STORE_ID_KEY = "storeId"

        val logger = KotlinLogging.logger {}
    }

    private val sessionStateMachines =
        ConcurrentHashMap<String, StateMachine<SessionState, SessionEvent>>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        logger.info { "연결 시작 -> ${session.id}" }

        sessionRegistry.register(session)

        val stateMachine = initializeStateMachine(session)

        logger.info { "${session.id} statemachine 생성 완료. 현재 상태 -> ${stateMachine.state.id}" }

        val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        session.attributes[SESSION_SCOPE_KEY] = sessionScope

        val replier = WebSocketReplier(session, sessionScope)

        session.attributes[REPLIER_KEY] = replier

        val clientVoiceStream = MutableSharedFlow<ByteArray>(
            extraBufferCapacity = 128, // 0.1초 마다 청크 보낼 시 12.8초 정도 저장 가능
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

        session.attributes[CLIENT_VOICE_STREAM_KEY] = clientVoiceStream

        val authentication = session.attributes[PRINCIPAL_KEY] as? UsernamePasswordAuthenticationToken

        val userDetails = authentication?.principal as? KioskUserDetails
            ?: run {
                logger.error { "올바르지 않은 authentication -> ${session.attributes[PRINCIPAL_KEY]}" }
                session.close(CloseStatus.POLICY_VIOLATION)
                return
            }

        val storeId = session.attributes[STORE_ID_KEY] as? Long
            ?: run {
                logger.error { "올바르지 않은 storeId -> ${session.attributes[STORE_ID_KEY]}" }
                session.close(CloseStatus.BAD_DATA)
                return
            }

        session.attributes[SHOPPING_CART_KEY] = ShoppingCartDTO(mutableListOf())

        logger.info { "쇼핑카트 생성 완료" }

        sessionScope.launch {
            val rtzrReadySignal = CompletableDeferred<Unit>()

            launch {
                try {
                    kioskSessionService.processVoiceChunk(
                        rtzrReadySignal,
                        clientVoiceStream,
                        storeId,
                        userDetails.memberInfo,
                        stateMachine,
                        session,
                        onVoiceChunk = replyVoiceChunk(session)
                    )
                } catch (_: CancellationException) {
                    logger.info { "세션 ${session.id} 처리가 정상적으로 취소되었습니다." }
                } catch (e: Exception) {
                    logger.error(e) { "voice chunk 처리 중 에러 -> ${session.id}" }
                }
            }

            rtzrReadySignal.await()

            eventPublisher.publishEvent(ServerReadyEvent(source = this, sessionId = session.id))
            logger.info { "클라이언트(${session.id})에게 준비 완료 신호 전송 (STT 연결 완료 후)" }
        }
    }

    override fun handleBinaryMessage(session: WebSocketSession, message: BinaryMessage) {
        val clientVoiceStream =
            session.attributes[CLIENT_VOICE_STREAM_KEY] as? MutableSharedFlow<ByteArray>

        val bytes = ByteArray(message.payload.remaining())
        message.payload.get(bytes)

        val emitted = clientVoiceStream?.tryEmit(bytes)

        if (emitted == false)
            logger.warn { "세션 ${session.id}의 음성 스트림 버퍼가 가득 찼습니다." }

    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        logger.info { "Client disconnected: ${session.id}, code: ${status.code}, reason: ${status.reason}" }

        (session.attributes[REPLIER_KEY] as? WebSocketReplier)?.close()

        (session.attributes[SESSION_SCOPE_KEY] as? CoroutineScope)?.cancel()

        sessionStateMachines.remove(session.id)

        sessionRegistry.unregister(session.id)

        kioskSessionService.cleanupSession(session.id)
    }

    private fun initializeStateMachine(session: WebSocketSession): StateMachine<SessionState, SessionEvent> {
        val stateMachine = stateMachineFactory.getStateMachine(session.id)
        stateMachine.startReactively().subscribe()

        sessionStateMachines[session.id] = stateMachine
        return stateMachine
    }

    private fun replyVoiceChunk(session: WebSocketSession): suspend (ByteArray) -> Unit =
        suspend@{ chunk ->
            val replier = session.attributes[REPLIER_KEY] as? WebSocketReplier

            val scope = session.attributes[SESSION_SCOPE_KEY] as? CoroutineScope

            if (replier == null || scope == null) {
                logger.warn { "Replier or Scope not found for session ${session.id}" }
                return@suspend
            }

            scope.launch {
                val result: Result<Unit> = replier.send(BinaryMessage(chunk))

                if (result.isFailure)
                    logger.warn(result.exceptionOrNull()) { "메세지 전송 실패 -> ${session.id}" }
            }
        }
}
