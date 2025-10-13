package jnu.ie.capstone.menu.dto.response

import jnu.ie.capstone.menu.dto.response.internal.OptionResponseDTO
import jnu.ie.capstone.menu.model.entity.Menu
import jnu.ie.capstone.menu.model.entity.Option

data class MenuResponse(
    val id: Long,
    val name: String,
    val price: Long,
    val options: List<OptionResponseDTO>? = null
) {
    companion object {
        fun from(menu: Menu, options: List<Option>? = null): MenuResponse {
            return MenuResponse(
                id = menu.id,
                name = menu.name.value,
                price = menu.price.value,
                options = options?.map { OptionResponseDTO.from(it) }
            )
        }
    }
}