package jnu.ie.capstone.menu.dto.request.internal

import jnu.ie.capstone.menu.dto.internal.interfaces.MenuDTO
import jnu.ie.capstone.menu.model.vo.MenuName
import jnu.ie.capstone.menu.model.vo.Price

data class CreateMenuRequestDTO(
    override val price: Price,
    override val name: MenuName,
    override val options: List<CreateOptionRequestDTO>? = null
) : MenuDTO