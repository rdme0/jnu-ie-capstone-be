package jnu.ie.capstone.menu.dto.request

import jnu.ie.capstone.menu.dto.request.interfaces.MenuRequest
import jnu.ie.capstone.menu.dto.request.internal.UpdateMenuRequestDTO

data class UpdateMenuRequest(
    override val menus: List<UpdateMenuRequestDTO>
) : MenuRequest



