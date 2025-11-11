package jnu.ie.capstone.rtzr.factory

import com.fasterxml.jackson.databind.ObjectMapper
import jnu.ie.capstone.rtzr.client.handler.RtzrSttWebSocketHandler
import jnu.ie.capstone.rtzr.dto.client.response.RtzrSttResponse
import kotlinx.coroutines.channels.SendChannel
import org.springframework.stereotype.Component

@Component
class RtzrSttWebsocketHandlerFactory(
    private val mapper: ObjectMapper
) {
    fun createHandler(channel: SendChannel<RtzrSttResponse>) : RtzrSttWebSocketHandler {
        return RtzrSttWebSocketHandler(channel, mapper)
    }
}