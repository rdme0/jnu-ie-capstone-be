package jnu.ie.capstone.rtzr.client.handler

import kotlinx.coroutines.channels.SendChannel
import mu.KotlinLogging
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

internal class RtzrSttWebSocketHandler(
    private val resultsChannel: SendChannel<String>
) : TextWebSocketHandler() {

    private val logger = KotlinLogging.logger {}

    override fun afterConnectionEstablished(session: WebSocketSession) {
        logger.info { "RTZR STT 연결 수립 -> ${session.id}" }
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val result = message.payload
        logger.debug { "STT 결과 수신: $result" }
        resultsChannel.trySend(result)
    }

    override fun afterConnectionClosed(
        session: WebSocketSession,
        status: org.springframework.web.socket.CloseStatus
    ) {
        logger.info { "RTZR STT 연결 종료 -> ${session.id}, Status: $status" }
        resultsChannel.close()
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        logger.error(exception) { "RTZR STT 연결 에러: ${session.id}" }
        resultsChannel.close(exception)
    }
}