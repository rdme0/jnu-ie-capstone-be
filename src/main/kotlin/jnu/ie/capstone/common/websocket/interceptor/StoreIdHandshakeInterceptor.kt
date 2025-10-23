package jnu.ie.capstone.common.websocket.interceptor

import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor
import java.lang.Exception

@Component
class StoreIdHandshakeInterceptor : HandshakeInterceptor {

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        val uriTemplateVars = request.uri.path.split("/")
        val storeIdIndex = uriTemplateVars.indexOf("stores") + 1

        if (storeIdIndex > 0 && storeIdIndex < uriTemplateVars.size) {
            val storeId = uriTemplateVars[storeIdIndex].toLongOrNull() ?: return false
            attributes["storeId"] = storeId
            return true
        }

        return false
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?
    ) {
    }
}