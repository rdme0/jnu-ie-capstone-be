package jnu.ie.capstone.menu.dto.request.internal

import jnu.ie.capstone.menu.dto.internal.interfaces.OptionDTO
import jnu.ie.capstone.menu.model.vo.OptionName
import jnu.ie.capstone.menu.model.vo.Price

data class UpdateOptionRequestDTO(
    val id: Long,
    override val price: Price,
    override val name: OptionName
) : OptionDTO