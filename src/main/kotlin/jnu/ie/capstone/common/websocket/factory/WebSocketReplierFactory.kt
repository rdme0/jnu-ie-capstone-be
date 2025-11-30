package jnu.ie.capstone.common.websocket.factory

import com.fasterxml.jackson.databind.ObjectMapper
import jnu.ie.capstone.common.websocket.util.WebSocketReplier
import kotlinx.coroutines.CoroutineScope
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession

@Component
class WebSocketReplierFactory(
    private val mapper: ObjectMapper
) {
    companion object {
        private const val CHANNEL_CAPACITY = 256
    }

    fun create(
        session: WebSocketSession,
        scope: CoroutineScope
    ): WebSocketReplier {
        return WebSocketReplier(mapper, session, scope, CHANNEL_CAPACITY)
    }
}