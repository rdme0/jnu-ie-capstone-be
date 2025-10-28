package jnu.ie.capstone.session.event.listener

import com.fasterxml.jackson.databind.ObjectMapper
import jnu.ie.capstone.common.config.ApplicationCoroutineScope
import jnu.ie.capstone.common.function.not
import jnu.ie.capstone.session.dto.response.SessionResponse
import jnu.ie.capstone.session.event.ApplicationSessionEvent
import jnu.ie.capstone.session.event.EndOfGeminiTurnEvent
import jnu.ie.capstone.session.event.OutputTextEvent
import jnu.ie.capstone.session.event.ServerReadyEvent
import jnu.ie.capstone.session.event.ShoppingCartUpdatedEvent
import jnu.ie.capstone.session.event.StateChangeEvent
import jnu.ie.capstone.session.registry.WebSocketSessionRegistry
import kotlinx.coroutines.launch
import org.springframework.context.event.EventListener
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
    fun handleServerReady(event: ServerReadyEvent) = scope.launch {
        val session = getSession(event) ?: return@launch

        val response = SessionResponse.fromServerReady()

        val payload = mapper.writeValueAsString(response)

        session.sendMessage(TextMessage(payload))

        logger.info { "세션 ID ${event.sessionId}로 준비 완료 메시지 전송 완료" }
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

        logger.debug { "세션 ID ${event.sessionId}로 GEMINI STT chunk 전송 완료" }
    }

    @EventListener
    fun handleEndOfGeminiTurn(event: EndOfGeminiTurnEvent) = scope.launch {
        val session = getSession(event) ?: return@launch

        val response = SessionResponse.fromEndOfGeminiTurn(event.content)

        val payload = mapper.writeValueAsString(response)

        session.sendMessage(TextMessage(payload))

        logger.info { "세션 ID ${event.sessionId}로 GEMINI STT result 전송 완료" }
    }

    @EventListener
    fun handleStateChange(event: StateChangeEvent) {
        scope.launch {
            val session = getSession(event.sessionId) ?: return@launch

            val response = SessionResponse.fromStateChange(event.content)
            val payload = mapper.writeValueAsString(response)

            session.sendMessage(TextMessage(payload))

            logger.info { "세션 ID ${event.sessionId}로 state 변경 메시지 전송 완료 (${event.content.from} -> ${event.content.to})" }
        }
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