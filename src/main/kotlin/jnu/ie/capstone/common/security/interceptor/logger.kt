package jnu.ie.capstone.common.security.interceptor

import jnu.ie.capstone.common.constant.CommonConstants.CRITICAL_ERROR_MESSAGE
import jnu.ie.capstone.common.exception.client.UnauthorizedException
import jnu.ie.capstone.common.exception.server.InternalServerException
import jnu.ie.capstone.common.security.helper.JwtAuthHelper
import mu.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand.*
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.core.AuthenticationException
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class WebSocketJwtAuthInterceptor(private val helper: JwtAuthHelper) : ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)
        val command = accessor?.command
            ?: throw InternalServerException(IllegalStateException("웹소켓 command가 null임"))

        if (command == CONNECT) {
            try {
                val authorizationHeaders = accessor.getNativeHeader(HttpHeaders.AUTHORIZATION)
                    ?: throw UnauthorizedException()
                val auth = helper.authenticate(authorizationHeaders.first())
                accessor.user = auth
            } catch (e: AuthenticationException) {
                val detailedMessage = accessor.getDetailedLogMessage(message.getPayload())
                logger.warn { "웹소켓 인증에 실패하였습니다 | 예외 메세지 -> ${e.stackTraceToString()} | 요청 -> $detailedMessage" }
                throw e
            } catch (e: Exception) {
                AuthenticationServiceException(CRITICAL_ERROR_MESSAGE.format("웹소켓 인증"), e)
            }
        }
        return message
    }

}