package jnu.ie.capstone.common.websocket.config

import jnu.ie.capstone.common.security.config.AllowedOriginsProperties
import jnu.ie.capstone.common.websocket.interceptor.JwtAuthHandshakeInterceptor
import jnu.ie.capstone.common.websocket.interceptor.QueryParmsInterceptor
import jnu.ie.capstone.session.handler.KioskAiSessionHandler
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val originsProperties: AllowedOriginsProperties,
    private val authInterceptor: JwtAuthHandshakeInterceptor,
    private val queryParmsInterceptor: QueryParmsInterceptor,
    private val handler: KioskAiSessionHandler
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(handler, "/websocket/voice")
            .addInterceptors(queryParmsInterceptor, authInterceptor)
            .setAllowedOriginPatterns(*originsProperties.allowedFrontEndOrigins.toTypedArray())
    }

}