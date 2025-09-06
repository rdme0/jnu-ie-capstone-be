package jnu.ie.capstone.common.websocket.exception.handler

import jnu.ie.capstone.common.exception.client.ClientException
import jnu.ie.capstone.common.exception.client.UnauthorizedException
import jnu.ie.capstone.common.exception.enums.ErrorCode
import mu.KotlinLogging
import org.springframework.messaging.Message
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler

@Component
class WebSocketExceptionHandler : StompSubProtocolErrorHandler() {
    private val logger = KotlinLogging.logger {}
    override fun handleClientMessageProcessingError(
        clientMessage: Message<ByteArray>?,
        ex: Throwable
    ): Message<ByteArray>? {
        val cause = ex.cause ?: ex

        return when (cause) {
            is UnauthorizedException -> {
                logger.warn { "UnauthorizedException이 StompSecurityExceptionHandler 에서 핸들링 됨 -> ${cause.message}" }
                setErrorMessage(cause.message)
            }

            is ClientException -> {
                logger.warn { "ClientException이 StompSecurityExceptionHandler 에서 핸들링 됨 -> ${cause.message}" }
                setErrorMessage(cause.message)
            }

            else -> {
                logger.warn { "알려지지 않은 예외가 StompSecurityExceptionHandler 에서 핸들링 됨 -> ${cause.message}" }
                setErrorMessage(ErrorCode.INTERNAL_SERVER.message)
            }
        }
    }

    private fun setErrorMessage(message: String?): Message<ByteArray> {
        val accessor = StompHeaderAccessor.create(StompCommand.ERROR)
        accessor.message = message
        accessor.setLeaveMutable(true)

        return MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
    }
}