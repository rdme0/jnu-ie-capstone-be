package jnu.ie.capstone.session.dto.internal

import jnu.ie.capstone.session.dto.response.WebSocketTextResponseContent

data class OutputTextResultDTO(
    val text: String
) : WebSocketTextResponseContent