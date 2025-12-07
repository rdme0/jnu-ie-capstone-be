package jnu.ie.capstone.session.dto.internal

import jnu.ie.capstone.session.dto.request.WebSocketTextRequestContent

data class ProcessPaymentDTO(
    val paymentMethod: String?
) : WebSocketTextRequestContent