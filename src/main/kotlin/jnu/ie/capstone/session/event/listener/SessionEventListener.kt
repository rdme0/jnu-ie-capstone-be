package jnu.ie.capstone.session.event.listener

import com.fasterxml.jackson.databind.ObjectMapper
import jnu.ie.capstone.common.config.ApplicationCoroutineScope
import jnu.ie.capstone.common.function.not
import jnu.ie.capstone.session.dto.internal.StateChangeDTO
import jnu.ie.capstone.session.dto.response.SessionResponse
import jnu.ie.capstone.session.enums.SessionEvent
import jnu.ie.capstone.session.enums.SessionState
import jnu.ie.capstone.session.event.ApplicationSessionEvent
import jnu.ie.capstone.session.event.EndOfGeminiTurnEvent
import jnu.ie.capstone.session.event.OutputTextEvent
import jnu.ie.capstone.session.event.ShoppingCartUpdatedEvent
import jnu.ie.capstone.session.registry.WebSocketSessionRegistry
import kotlinx.coroutines.launch
import org.springframework.context.event.EventListener
import org.springframework.statemachine.StateContext
import org.springframework.statemachine.annotation.OnStateChanged
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession

@Component
class SessionEventListener(
    private val scope: ApplicationCoroutineScope,
    private val sessionRegistry: WebSocketSessionRegistry,
    private val mapper: ObjectMapper
) {

    companion object {
        private val logger = mu.KotlinLogging.logger {}
    }

    @EventListener
    fun handleShoppingCartUpdate(event: ShoppingCartUpdatedEvent) = scope.launch {
        val session = getSession(event) ?: return@launch

        val response = SessionResponse.fromUpdateShoppingCart(event.content)

        val payload = mapper.writeValueAsString(response)

        session.sendMessage(TextMessage(payload))

        logger.info { "세션 ID ${event.sessionId}로 장바구니 업데이트 전송 완료." }
    }


    @EventListener
    fun handleOutputText(event: OutputTextEvent) = scope.launch {
        val session = getSession(event) ?: return@launch

        val response = SessionResponse.fromOutputText(event.content)

        val payload = mapper.writeValueAsString(response)

        session.sendMessage(TextMessage(payload))

        logger.info { "세션 ID ${event.sessionId}로 GEMINI STT chunk 전송 완료" }
    }

    @EventListener
    fun handleEndOfGeminiTurn(event: EndOfGeminiTurnEvent) = scope.launch {
        val session = getSession(event) ?: return@launch

        val response = SessionResponse.fromEndOfGeminiTurn(event.content)

        val payload = mapper.writeValueAsString(response)

        session.sendMessage(TextMessage(payload))

        logger.info { "세션 ID ${event.sessionId}로 GEMINI STT result 전송 완료" }
    }


    @OnStateChanged
    fun onStateChanged(context: StateContext<SessionState, SessionEvent>) = scope.launch {
        val session = getSession(context.stateMachine.id) ?: return@launch

        val fromState = context.source?.id
        val toState = context.target.id
        val because = context.event

        val content = StateChangeDTO(fromState, toState, because)
        val response = SessionResponse.fromStateChange(content)
        val payload = mapper.writeValueAsString(response)

        session.sendMessage(TextMessage(payload))

        logger.info { "세션 ID ${session.id}로 state 변경 메시지 전송 완료" }
    }


    private fun getSession(id: String): WebSocketSession? {
        val session = sessionRegistry.getSession(id) ?: run {
            logger.warn { ">>> session not found. sessionId: $id" }
            return null
        }

        if (not(session.isOpen)) {
            logger.warn { ">>> session is not open. sessionId: $id" }
            return null
        }

        return session
    }


    private fun getSession(event: ApplicationSessionEvent): WebSocketSession? {
        return getSession(event.sessionId)
    }

}