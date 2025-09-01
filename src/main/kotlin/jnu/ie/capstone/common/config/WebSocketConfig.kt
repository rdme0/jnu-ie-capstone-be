package jnu.ie.capstone.common.config

import jnu.ie.capstone.common.security.config.AllowedOriginsProperties
import jnu.ie.capstone.common.security.exception.handler.WebSocketExceptionHandler
import jnu.ie.capstone.common.security.interceptor.WebSocketJwtAuthInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    private val originsProperties: AllowedOriginsProperties,
    private val authInterceptor: WebSocketJwtAuthInterceptor,
    private val exceptionHandler: WebSocketExceptionHandler
) : WebSocketMessageBrokerConfigurer {

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/websocket/connect")
            .setAllowedOriginPatterns(*originsProperties.allowedFrontEndOrigins.toTypedArray())
            .withSockJS()

        registry.setErrorHandler(exceptionHandler)
    }

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/subscribe")
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(authInterceptor)
    }

}