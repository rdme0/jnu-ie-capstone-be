package jnu.ie.capstone.session.dto.internal

import jnu.ie.capstone.session.dto.response.SessionResponseContent
import jnu.ie.capstone.session.enums.SessionEvent
import jnu.ie.capstone.session.enums.SessionState

data class StateChangeDTO(
    val from: SessionState?,
    val to: SessionState,
    val because: SessionEvent?
) : SessionResponseContent