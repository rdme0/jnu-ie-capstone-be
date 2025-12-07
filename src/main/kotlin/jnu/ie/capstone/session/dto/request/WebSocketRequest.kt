package jnu.ie.capstone.session.dto.request

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import jnu.ie.capstone.session.dto.internal.ProcessPaymentDTO
import jnu.ie.capstone.session.enums.MessageType

interface WebSocketTextRequestContent

@ConsistentCopyVisibility
data class WebSocketTextRequest private constructor(
    val messageType: MessageType,

    @param:JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
        property = "messageType",
        visible = true
    )
    @param:JsonSubTypes(
        JsonSubTypes.Type(value = ProcessPaymentDTO::class, name = "PROCESS_PAYMENT")
    )
    val content: WebSocketTextRequestContent
)