package jnu.ie.capstone.session.event.listener

import com.fasterxml.jackson.databind.ObjectMapper
import jnu.ie.capstone.common.config.ApplicationCoroutineScope
import jnu.ie.capstone.common.function.not
import jnu.ie.capstone.common.websocket.util.WebSocketReplier
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

        val replier = getReplier(session) ?: return@launch

        val response = SessionResponse.fromServerReady()

        replyText(response, replier, event)
    }

    @EventListener
    fun handleShoppingCartUpdate(event: ShoppingCartUpdatedEvent) = scope.launch {
        val session = getSession(event) ?: return@launch

        val replier = getReplier(session) ?: return@launch

        val response = SessionResponse.fromUpdateShoppingCart(event.content)

        replyText(response, replier, event)
    }


    @EventListener
    fun handleOutputText(event: OutputTextEvent) = scope.launch {
        val session = getSession(event) ?: return@launch

        val replier = getReplier(session) ?: return@launch

        val response = SessionResponse.fromOutputText(event.content)

        replyText(response, replier, event)
    }

    @EventListener
    fun handleEndOfGeminiTurn(event: EndOfGeminiTurnEvent) = scope.launch {
        val session = getSession(event) ?: return@launch

        val replier = getReplier(session) ?: return@launch

        val response = SessionResponse.fromEndOfGeminiTurn(event.content)

        replyText(response, replier, event)
    }

    @EventListener
    fun handleStateChange(event: StateChangeEvent) {
        scope.launch {
            val session = getSession(event.sessionId) ?: return@launch

            val replier = getReplier(session) ?: return@launch

            val response = SessionResponse.fromStateChange(event.content)

            replyText(response, replier, event)
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

    private fun getReplier(session: WebSocketSession): WebSocketReplier? {
        return session.attributes["replier"] as? WebSocketReplier ?: run {
            logger.warn { ">>> replier not found. sessionId: ${session.id}" }
            return null
        }
    }

    private suspend fun replyText(
        response: SessionResponse,
        replier: WebSocketReplier,
        event: ApplicationSessionEvent
    ) {
        val payload = mapper.writeValueAsString(response)

        logger.debug { ">>> response payload: $payload" }

        val result = replier.send(TextMessage(payload))

        when {
            result.isSuccess -> {
                logger.info { "세션 ID ${event.sessionId}로 ${event.content} 메시지 전송 성공" }
            }

            result.isFailure -> {
                logger.warn(result.exceptionOrNull()) { "세션 ID ${event.sessionId}로  ${event.content} 메시지 전송 실패" }
            }
        }
    }
}