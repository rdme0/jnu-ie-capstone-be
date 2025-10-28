package jnu.ie.capstone.session.dto.internal

import jnu.ie.capstone.session.dto.response.SessionResponseContent
import jnu.ie.capstone.session.enums.SessionState

data class ServerReadyDTO(
    val state: SessionState = SessionState.MENU_SELECTION
) : SessionResponseContent