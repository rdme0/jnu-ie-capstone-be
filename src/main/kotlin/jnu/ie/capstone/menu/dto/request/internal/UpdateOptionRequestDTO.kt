package jnu.ie.capstone.menu.dto.request.internal

import jnu.ie.capstone.menu.model.vo.OptionName
import jnu.ie.capstone.menu.model.vo.Price

data class UpdateOptionRequestDTO(
    val price: Price,
    val name: OptionName
)