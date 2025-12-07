package jnu.ie.capstone.session.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jnu.ie.capstone.common.security.dto.KioskUserDetails
import jnu.ie.capstone.common.websocket.factory.WebSocketReplierFactory
import jnu.ie.capstone.common.websocket.util.WebSocketReplier
import jnu.ie.capstone.session.dto.internal.ShoppingCartDTO
import jnu.ie.capstone.session.dto.request.WebSocketTextRequest
import jnu.ie.capstone.session.dto.response.WebSocketResponse
import jnu.ie.capstone.session.dto.response.WebSocketTextResponse
import jnu.ie.capstone.session.enums.MessageType
import jnu.ie.capstone.session.enums.SessionEvent
import jnu.ie.capstone.session.enums.SessionState
import jnu.ie.capstone.session.registry.WebSocketSessionRegistry
import jnu.ie.capstone.session.service.KioskSessionService
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
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.BinaryWebSocketHandler
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set
import kotlin.coroutines.cancellation.CancellationException

@Component
class KioskAiSessionHandler(
    private val service: KioskSessionService,
    private val stateMachineFactory: StateMachineFactory<SessionState, SessionEvent>,
    private val replierFactory: WebSocketReplierFactory,
    private val sessionRegistry: WebSocketSessionRegistry,
    private val mapper: ObjectMapper
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

        val webSocketSessionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        session.attributes[SESSION_SCOPE_KEY] = webSocketSessionScope

        val replier: WebSocketReplier = replierFactory.create(session, webSocketSessionScope)

        session.attributes[REPLIER_KEY] = replier

        val clientVoiceStream = MutableSharedFlow<ByteArray>(
            extraBufferCapacity = 128, // 0.1초 마다 청크 보낼 시 12.8초 정도 저장 가능
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

        session.attributes[CLIENT_VOICE_STREAM_KEY] = clientVoiceStream

        val authentication =
            session.attributes[PRINCIPAL_KEY] as? UsernamePasswordAuthenticationToken

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

        webSocketSessionScope.launch {
            val geminiReadySignal = CompletableDeferred<Unit>()

            launch {
                try {
                    service.processVoiceChunk(
                        geminiReadySignal,
                        clientVoiceStream,
                        storeId,
                        userDetails.memberInfo,
                        stateMachine,
                        session
                    ) { message -> reply(session)(message) }
                } catch (_: CancellationException) {
                    logger.info { "세션 ${session.id} 처리가 정상적으로 취소되었습니다." }
                } catch (e: Exception) {
                    logger.error(e) { "voice chunk 처리 중 에러 -> ${session.id}" }
                }
            }

            geminiReadySignal.await()

            replier.send(WebSocketTextResponse.fromServerReady())

            logger.info { "클라이언트(${session.id})에게 준비 완료 신호 전송" }
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

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val payload = message.payload

        logger.info { "Client sent (Text): $payload" }

        val request: WebSocketTextRequest = runCatching {
            mapper.readValue<WebSocketTextRequest>(payload)
        }
            .onFailure {
                logger.error(it) { "TextMessage parsing error: $payload" }
                return
            }
            .getOrNull() ?: return

        val stateMachine = sessionStateMachines[session.id]
            ?: run {
                logger.error { "state machine not found" }
                return
            }

        val webSocketSessionScope = session.attributes[SESSION_SCOPE_KEY] as? CoroutineScope
            ?: run {
                logger.error { "session scope not found" }
                return
            }

        when (request.messageType) {
            MessageType.PROCESS_PAYMENT -> {
                webSocketSessionScope.launch {
                    service.processPayment(stateMachine = stateMachine, onReply = reply(session))
                }
            }

            else -> {}
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        logger.info { "Client disconnected: ${session.id}, code: ${status.code}, reason: ${status.reason}" }

        (session.attributes[REPLIER_KEY] as? WebSocketReplier)?.close()

        (session.attributes[SESSION_SCOPE_KEY] as? CoroutineScope)?.cancel()

        sessionStateMachines.remove(session.id)

        sessionRegistry.unregister(session.id)

        service.cleanupSession(session.id)
    }

    private fun initializeStateMachine(session: WebSocketSession): StateMachine<SessionState, SessionEvent> {
        val stateMachine = stateMachineFactory.getStateMachine(session.id)
        stateMachine.startReactively().subscribe()

        sessionStateMachines[session.id] = stateMachine
        return stateMachine
    }

    private fun reply(session: WebSocketSession): suspend (WebSocketResponse) -> Unit =
        suspend@{ message ->
            val replier = session.attributes[REPLIER_KEY] as? WebSocketReplier

            val scope = session.attributes[SESSION_SCOPE_KEY] as? CoroutineScope

            if (replier == null || scope == null) {
                logger.warn { "Replier or Scope not found for session ${session.id}" }
                return@suspend
            }

            val result: Result<Unit> = replier.send(message)

            if (result.isFailure)
                logger.warn(result.exceptionOrNull()) { "메세지 전송 실패 -> ${session.id}" }
        }
}
