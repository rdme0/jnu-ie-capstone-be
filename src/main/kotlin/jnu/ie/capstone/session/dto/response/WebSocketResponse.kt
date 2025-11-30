package jnu.ie.capstone.session.dto.response

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import jnu.ie.capstone.session.dto.internal.OutputTextChunkDTO
import jnu.ie.capstone.session.dto.internal.OutputTextResultDTO
import jnu.ie.capstone.session.dto.internal.ServerReadyDTO
import jnu.ie.capstone.session.dto.internal.ShoppingCartResponseDTO
import jnu.ie.capstone.session.dto.internal.StateChangeDTO
import jnu.ie.capstone.session.enums.MessageType

sealed interface WebSocketResponse {
    val content: Any
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = ServerReadyDTO::class, name = "serverReady"),
    JsonSubTypes.Type(value = ShoppingCartResponseDTO::class, name = "updateShoppingCart"),
    JsonSubTypes.Type(value = OutputTextChunkDTO::class, name = "outputTextChunk"),
    JsonSubTypes.Type(value = OutputTextResultDTO::class, name = "outputTextResult"),
    JsonSubTypes.Type(value = StateChangeDTO::class, name = "changeState")
)
interface WebSocketTextResponseContent

@Suppress("ArrayInDataClass")
data class WebSocketBinaryResponse(
    override val content: ByteArray
) : WebSocketResponse

@ConsistentCopyVisibility
data class WebSocketTextResponse private constructor(
    val messageType: MessageType,
    override val content: WebSocketTextResponseContent
) : WebSocketResponse {
    companion object {
        fun fromServerReady(): WebSocketTextResponse {
            return WebSocketTextResponse(MessageType.SERVER_READY, ServerReadyDTO())
        }

        fun fromUpdateShoppingCart(content: ShoppingCartResponseDTO): WebSocketTextResponse {
            return WebSocketTextResponse(MessageType.UPDATE_SHOPPING_CART, content)
        }

        fun fromOutputText(content: OutputTextChunkDTO): WebSocketTextResponse {
            return WebSocketTextResponse(MessageType.OUTPUT_TEXT_CHUNK, content)
        }

        fun fromEndOfGeminiTurn(content: OutputTextResultDTO): WebSocketTextResponse {
            return WebSocketTextResponse(MessageType.OUTPUT_TEXT_RESULT, content)
        }

        fun fromStateChange(content: StateChangeDTO): WebSocketTextResponse {
            return WebSocketTextResponse(MessageType.CHANGE_STATE, content)
        }
    }
}