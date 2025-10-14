package jnu.ie.capstone.common.websocket.interceptor

import mu.KotlinLogging
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor
import org.springframework.web.util.UriComponentsBuilder
import java.lang.Exception

@Component
class QueryParmsInterceptor() : HandshakeInterceptor {
    private val logger = KotlinLogging.logger {}

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        val requestUri = UriComponentsBuilder.fromUri(request.uri)
            .build()

        attributes["storeId"] = requestUri
            .queryParams
            .getFirst("storeId")?.toLongOrNull() ?: return false

        logger.info { "storeId: ${attributes["storeId"]}" }

        return true
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?
    ) {
    }
}