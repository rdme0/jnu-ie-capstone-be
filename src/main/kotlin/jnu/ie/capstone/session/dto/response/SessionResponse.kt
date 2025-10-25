package jnu.ie.capstone.session.dto.response

import jnu.ie.capstone.session.dto.internal.OutputTextChunkDTO
import jnu.ie.capstone.session.dto.internal.OutputTextResultDTO
import jnu.ie.capstone.session.dto.internal.ShoppingCartResponseDTO
import jnu.ie.capstone.session.dto.internal.StateChangeDTO
import jnu.ie.capstone.session.enums.MessageType

@ConsistentCopyVisibility
data class SessionResponse private constructor(
    val messageType: MessageType,
    val content: SessionResponseContent
) {
    companion object {
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

interface SessionResponseContent