package jnu.ie.capstone.common.websocket.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession

class WebSocketReplier(
    private val session: WebSocketSession,
    scope: CoroutineScope,
    channelCapacity: Int = 256
) {
    private val logger = KotlinLogging.logger {}
    private val queue =
        Channel<WebSocketMessage<*>>(capacity = channelCapacity) //gemini 응답은 요청보다 더 길 것으로 보아 128 * 2로 함

    init {
        scope.launch {
            try {
                for (message in queue) { // queue가 빌 경우 대기 함
                    if (session.isOpen) {
                        session.sendMessage(message)
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "websocket replier error" }
            } finally {
                queue.close()
            }
        }
    }

    suspend fun send(message: WebSocketMessage<*>): Result<Unit> {
        return runCatching {
            queue.send(message)
        }
    }

    fun close() = queue.close()

}