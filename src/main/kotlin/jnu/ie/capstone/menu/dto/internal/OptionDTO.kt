package jnu.ie.capstone.menu.dto.internal

import jnu.ie.capstone.menu.model.vo.OptionName
import jnu.ie.capstone.menu.model.vo.Price

interface OptionDTO {
    val price: Price
    val name: OptionName
}