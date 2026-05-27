package jnu.ie.capstone.common.websocket.util

import com.fasterxml.jackson.databind.ObjectMapper
import jnu.ie.capstone.session.dto.response.WebSocketBinaryResponse
import jnu.ie.capstone.session.dto.response.WebSocketResponse
import jnu.ie.capstone.session.dto.response.WebSocketTextResponse
import mu.KotlinLogging
import org.springframework.web.socket.BinaryMessage
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class WebSocketBlockingReplier(
    private val mapper: ObjectMapper,
    private val session: WebSocketSession,
    private val channelCapacity: Int = 256
) : AutoCloseable {
    private val logger = KotlinLogging.logger {}
    private val running = AtomicBoolean(true)
    private val queue: BlockingQueue<WebSocketResponse> = LinkedBlockingQueue(channelCapacity)

    fun send(message: WebSocketResponse): Result<Unit> {
        return runCatching {
            if (!queue.offer(message, 5, TimeUnit.SECONDS)) {
                throw IllegalStateException("blocking websocket reply queue is full")
            }
        }
    }

    fun drain() {
        while (running.get() && session.isOpen) {
            val message = queue.poll(500, TimeUnit.MILLISECONDS) ?: continue
            try {
                when (message) {
                    is WebSocketBinaryResponse -> {
                        logger.debug { ">>> blocking byte response -> ${message.content.size}byte" }
                        session.sendMessage(BinaryMessage(message.content))
                    }

                    is WebSocketTextResponse -> {
                        val payload = mapper.writeValueAsString(message)
                        logger.debug { ">>> blocking response payload: $payload" }
                        session.sendMessage(TextMessage(payload))
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "blocking websocket replier error" }
                close()
            }
        }
    }

    override fun close() {
        running.set(false)
    }
}
