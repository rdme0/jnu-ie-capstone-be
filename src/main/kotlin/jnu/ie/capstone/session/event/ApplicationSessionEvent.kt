package jnu.ie.capstone.session.event

import jnu.ie.capstone.session.dto.internal.OutputTextChunkDTO
import jnu.ie.capstone.session.dto.internal.OutputTextResultDTO
import jnu.ie.capstone.session.dto.internal.ShoppingCartResponseDTO
import jnu.ie.capstone.session.dto.response.SessionResponseContent
import org.springframework.context.ApplicationEvent

sealed class ApplicationSessionEvent(
    source: Any,
    open val sessionId: String,
    open val content: SessionResponseContent
) : ApplicationEvent(source)

class ShoppingCartUpdatedEvent(
    source: Any,
    override val sessionId: String,
    override val content: ShoppingCartResponseDTO
) : ApplicationSessionEvent(source, sessionId, content)

class OutputTextEvent(
    source: Any,
    override val sessionId: String,
    override val content: OutputTextChunkDTO
) : ApplicationSessionEvent(source, sessionId, content)

class EndOfGeminiTurnEvent(
    source: Any,
    override val sessionId: String,
    override val content: OutputTextResultDTO
) : ApplicationSessionEvent(source, sessionId, content)