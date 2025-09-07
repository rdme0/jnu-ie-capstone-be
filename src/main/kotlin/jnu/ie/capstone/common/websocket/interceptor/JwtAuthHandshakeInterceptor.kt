package jnu.ie.capstone.common.websocket.interceptor

import jnu.ie.capstone.common.exception.client.ClientException
import jnu.ie.capstone.common.exception.client.UnauthorizedException
import jnu.ie.capstone.common.exception.enums.ErrorCode
import jnu.ie.capstone.common.security.helper.JwtAuthHelper
import mu.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor
import java.lang.Exception

@Component
class JwtAuthHandshakeInterceptor(
    private val helper: JwtAuthHelper
) : HandshakeInterceptor {
    private val logger = KotlinLogging.logger {}

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        if (request !is ServletServerHttpRequest) {
            logger.warn { "ServletServerHttpRequest 타입이 아닙니다. 핸드셰이크를 거부합니다." }
            return false
        }

        try {
            val authHeader = request.servletRequest.getHeader(HttpHeaders.AUTHORIZATION)
            val auth: Authentication = helper.authenticate(authHeader)

            attributes["principal"] = auth
            logger.info { "WebSocket 핸드셰이크 인증 성공: User=${auth.name}" }

            return true
        } catch (ex: Exception) {
            val cause = ex.cause ?: ex
            logger.warn { "WebSocket 핸드셰이크 인증 실패: ${cause.message}" }

            response.setStatusCode(HttpStatus.UNAUTHORIZED)

            val errorMessage = when (cause) {
                is UnauthorizedException, is ClientException -> cause.message
                else -> ErrorCode.INTERNAL_SERVER.message
            }

            response.body.write(errorMessage?.toByteArray())

            return false
        }
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?
    ) {

    }
}