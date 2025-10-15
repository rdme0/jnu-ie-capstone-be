package jnu.ie.capstone.menu.dto.request

import jnu.ie.capstone.menu.model.vo.MenuName
import jnu.ie.capstone.menu.model.vo.Price

data class UpdateMenuRequest(
    val price: Price,
    val name: MenuName,
)