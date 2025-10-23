package jnu.ie.capstone.session.dto.internal

import jnu.ie.capstone.session.dto.response.SessionResponseContent

data class OutputTextChunkDTO(
    val text: String
) : SessionResponseContent