package jnu.ie.capstone.session.registry

import org.springframework.stereotype.Service
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

@Service
class WebSocketSessionRegistry {
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()

    fun register(session: WebSocketSession) {
        sessions[session.id] = session
    }

    fun unregister(sessionId: String) {
        sessions.remove(sessionId)
    }

    fun getSession(sessionId: String): WebSocketSession? {
        return sessions[sessionId]
    }
}