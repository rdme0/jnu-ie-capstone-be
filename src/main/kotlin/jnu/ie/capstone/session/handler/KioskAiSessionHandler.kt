package jnu.ie.capstone.session.handler

import jnu.ie.capstone.common.security.dto.KioskUserDetails
import jnu.ie.capstone.session.dto.internal.ShoppingCartDTO
import jnu.ie.capstone.session.enums.SessionEvent
import jnu.ie.capstone.session.enums.SessionState
import jnu.ie.capstone.session.event.ServerReadyEvent
import jnu.ie.capstone.session.event.listener.SessionEventListener
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
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val sessionStateMachines =
        ConcurrentHashMap<String, StateMachine<SessionState, SessionEvent>>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        logger.info { "연결 시작 -> ${session.id}" }

        sessionRegistry.register(session)

        val stateMachine = initializeStateMachine(session)

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

        session.attributes["shoppingCart"] = ShoppingCartDTO(mutableListOf())

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
                        onVoiceChunk = { chunk ->
                            if (session.isOpen) {
                                session.sendMessage(BinaryMessage(chunk))
                            }
                        }
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

        session.attributes["clientVoiceStream"] = clientVoiceStream
        session.attributes["sessionScope"] = sessionScope
    }

    override fun handleBinaryMessage(session: WebSocketSession, message: BinaryMessage) {
        val clientVoiceStream =
            session.attributes["clientVoiceStream"] as? MutableSharedFlow<ByteArray>

        val bytes = ByteArray(message.payload.remaining())
        message.payload.get(bytes)

        val emitted = clientVoiceStream?.tryEmit(bytes)

        if (emitted == false)
            logger.warn { "세션 ${session.id}의 음성 스트림 버퍼가 가득 찼습니다." }

    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        logger.info { "Client disconnected: ${session.id}, code: ${status.code}, reason: ${status.reason}" }

        (session.attributes["sessionScope"] as? CoroutineScope)?.cancel()

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

}
