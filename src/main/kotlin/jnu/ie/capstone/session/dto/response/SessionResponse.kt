package jnu.ie.capstone.session.dto.response

import jnu.ie.capstone.session.dto.internal.OutputTextChunkDTO
import jnu.ie.capstone.session.dto.internal.OutputTextResultDTO
import jnu.ie.capstone.session.dto.internal.ShoppingCartDTO
import jnu.ie.capstone.session.enums.MessageType

@ConsistentCopyVisibility
data class SessionResponse private constructor(
    val messageType: MessageType,
    val content: SessionResponseContent
) {
    companion object {
        fun fromUpdateShoppingCart(content: ShoppingCartDTO): SessionResponse {
            return SessionResponse(MessageType.UPDATE_SHOPPING_CART, content)
        }

        fun fromOutputText(content: OutputTextChunkDTO): SessionResponse {
            return SessionResponse(MessageType.OUTPUT_TEXT_CHUNK, content)
        }

        fun fromEndOfGeminiTurn(content: OutputTextResultDTO): SessionResponse {
            return SessionResponse(MessageType.OUTPUT_TEXT_RESULT, content)
        }
    }
}

interface SessionResponseContent