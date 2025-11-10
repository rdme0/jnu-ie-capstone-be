package jnu.ie.capstone.common.websocket.interceptor

import jnu.ie.capstone.common.security.dto.KioskUserDetails
import jnu.ie.capstone.store.service.StoreService
import mu.KotlinLogging
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor
import java.lang.Exception

@Component
class StoreIdHandshakeInterceptor(
    private val storeService: StoreService
) : HandshakeInterceptor {
    private companion object {
        val logger = KotlinLogging.logger {}
    }

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        val uriTemplateVars = request.uri.path.split("/")
        val storeIdIndex = uriTemplateVars.indexOf("stores") + 1
        val authentication = attributes["principal"] as? UsernamePasswordAuthenticationToken

        val userDetails = authentication?.principal as? KioskUserDetails
            ?: run {
                logger.error { "올바르지 않은 authentication -> ${attributes["principal"]}" }
                return false
            }

        if (storeIdIndex > 0 && storeIdIndex < uriTemplateVars.size) {
            val storeId = uriTemplateVars[storeIdIndex].toLongOrNull() ?: return false

            storeService.getBy(id = storeId, ownerId = userDetails.memberInfo.id)
                ?: run {
                    logger.warn { "올바르지 않은 store id -> ${storeId}, user -> ${userDetails.memberInfo}" }
                    return false
                }

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