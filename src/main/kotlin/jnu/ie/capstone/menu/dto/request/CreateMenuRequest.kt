package jnu.ie.capstone.menu.dto.request

import jnu.ie.capstone.menu.dto.request.interfaces.MenuRequest
import jnu.ie.capstone.menu.dto.request.internal.CreateMenuRequestDTO

data class CreateMenuRequest(
    override val menus: List<CreateMenuRequestDTO>
) : MenuRequest



