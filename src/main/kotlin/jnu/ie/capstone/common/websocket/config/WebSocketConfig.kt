package jnu.ie.capstone.common.websocket.config

import jnu.ie.capstone.common.security.config.UriSecurityConfig
import jnu.ie.capstone.common.websocket.interceptor.JwtAuthHandshakeInterceptor
import jnu.ie.capstone.common.websocket.interceptor.StoreIdHandshakeInterceptor
import jnu.ie.capstone.session.handler.KioskAiSessionHandler
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val originsProperties: UriSecurityConfig,
    private val authInterceptor: JwtAuthHandshakeInterceptor,
    private val storeIdHandshakeInterceptor: StoreIdHandshakeInterceptor,
    private val handler: KioskAiSessionHandler
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(handler, "/stores/{storeId}/websocket/kioskSession")
            .addInterceptors(authInterceptor, storeIdHandshakeInterceptor)
            .setAllowedOriginPatterns(*originsProperties.allowedFrontEndOrigins.toTypedArray())
    }

}