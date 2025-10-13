package jnu.ie.capstone.menu.dto.response.internal

import jnu.ie.capstone.menu.model.entity.Option

data class OptionResponseDTO(
    val id: Long,
    val price: Long,
    val name: String
) {
    companion object {
        fun from(option: Option): OptionResponseDTO {
            return OptionResponseDTO(
                id = option.id,
                price = option.price.value,
                name = option.name.value
            )
        }
    }
}