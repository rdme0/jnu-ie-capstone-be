package jnu.ie.capstone.session.dto.request

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import jnu.ie.capstone.session.dto.internal.ProcessPaymentDTO
import jnu.ie.capstone.session.enums.MessageType

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = ProcessPaymentDTO::class, name = "processPayment")
)
interface WebSocketTextRequestContent


@ConsistentCopyVisibility
data class WebSocketTextRequest private constructor(
    val messageType: MessageType,
    val content: WebSocketTextRequestContent
)