package jnu.ie.capstone.session.event.listener

import com.fasterxml.jackson.databind.ObjectMapper
import jnu.ie.capstone.common.config.ApplicationCoroutineScope
import jnu.ie.capstone.common.function.not
import jnu.ie.capstone.session.dto.response.SessionResponse
import jnu.ie.capstone.session.event.ApplicationSessionEvent
import jnu.ie.capstone.session.event.EndOfGeminiTurnEvent
import jnu.ie.capstone.session.event.OutputTextEvent
import jnu.ie.capstone.session.event.ShoppingCartUpdatedEvent
import jnu.ie.capstone.session.registry.WebSocketSessionRegistry
import kotlinx.coroutines.launch
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession

@Component
class ShoppingCartEventListener(
    private val scope: ApplicationCoroutineScope,
    private val sessionRegistry: WebSocketSessionRegistry,
    private val mapper: ObjectMapper
) {

    companion object {
        private val logger = mu.KotlinLogging.logger {}
    }

    @EventListener
    fun handleShoppingCartUpdate(event: ShoppingCartUpdatedEvent) {
        val session = getSession(event) ?: return

        scope.launch {
            val response = SessionResponse.fromUpdateShoppingCart(event.content)

            val payload = mapper.writeValueAsString(response)

            session.sendMessage(TextMessage(payload))

            logger.info { "세션 ID ${event.sessionId}로 장바구니 업데이트 전송 완료." }
        }
    }

    @EventListener
    fun handleOutputText(event: OutputTextEvent) {
        val session = getSession(event) ?: return

        scope.launch {
            val response = SessionResponse.fromOutputText(event.content)

            val payload = mapper.writeValueAsString(response)

            session.sendMessage(TextMessage(payload))

            logger.info { "세션 ID ${event.sessionId}로 GEMINI STT chunk 전송 완료" }
        }
    }

    @EventListener
    fun handleEndOfGeminiTurn(event: EndOfGeminiTurnEvent) {
        val session = getSession(event) ?: return

        scope.launch {
            val response = SessionResponse.fromEndOfGeminiTurn(event.content)

            val payload = mapper.writeValueAsString(response)

            session.sendMessage(TextMessage(payload))

            logger.info { "세션 ID ${event.sessionId}로 GEMINI STT 전송 완료" }
        }
    }

    private fun getSession(event: ApplicationSessionEvent): WebSocketSession? {
        val session = sessionRegistry.getSession(event.sessionId) ?: run {
            logger.warn { ">>> session not found. sessionId: ${event.sessionId}" }
            return null
        }

        if (not(session.isOpen)) {
            logger.warn { ">>> session is not open. sessionId: ${event.sessionId}" }
            return null
        }

        return session
    }
}