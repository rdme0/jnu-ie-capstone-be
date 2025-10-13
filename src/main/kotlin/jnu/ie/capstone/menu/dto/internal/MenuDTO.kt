package jnu.ie.capstone.menu.dto.internal

import jnu.ie.capstone.menu.model.vo.MenuName
import jnu.ie.capstone.menu.model.vo.Price

interface MenuDTO {
    val name: MenuName
    val price: Price
    val options: List<OptionDTO>?
}