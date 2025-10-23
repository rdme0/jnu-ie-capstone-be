package jnu.ie.capstone.common.websocket.config

import jnu.ie.capstone.common.websocket.constant.WebSocketConstant.BUFFER_SIZE
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean

@Configuration
class WebSocketBufferConfig {

    @Bean
    fun createWebSocketContainer(): ServletServerContainerFactoryBean {
        val container = ServletServerContainerFactoryBean()
        container.setMaxTextMessageBufferSize(BUFFER_SIZE)
        container.setMaxBinaryMessageBufferSize(BUFFER_SIZE)
        return container
    }

}