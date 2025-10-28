package jnu.ie.capstone.session.dto.response

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import jnu.ie.capstone.session.dto.internal.OutputTextChunkDTO
import jnu.ie.capstone.session.dto.internal.OutputTextResultDTO
import jnu.ie.capstone.session.dto.internal.ServerReadyDTO
import jnu.ie.capstone.session.dto.internal.ShoppingCartResponseDTO
import jnu.ie.capstone.session.dto.internal.StateChangeDTO
import jnu.ie.capstone.session.enums.MessageType

@ConsistentCopyVisibility
data class SessionResponse private constructor(
    val messageType: MessageType,
    val content: SessionResponseContent
) {
    companion object {
        fun fromServerReady(): SessionResponse {
            return SessionResponse(MessageType.SERVER_READY, ServerReadyDTO())
        }

        fun fromUpdateShoppingCart(content: ShoppingCartResponseDTO): SessionResponse {
            return SessionResponse(MessageType.UPDATE_SHOPPING_CART, content)
        }

        fun fromOutputText(content: OutputTextChunkDTO): SessionResponse {
            return SessionResponse(MessageType.OUTPUT_TEXT_CHUNK, content)
        }

        fun fromEndOfGeminiTurn(content: OutputTextResultDTO): SessionResponse {
            return SessionResponse(MessageType.OUTPUT_TEXT_RESULT, content)
        }

        fun fromStateChange(content: StateChangeDTO): SessionResponse {
            return SessionResponse(MessageType.CHANGE_STATE, content)
        }
    }
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
interface SessionResponseContent