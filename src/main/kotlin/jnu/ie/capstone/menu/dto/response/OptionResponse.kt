package jnu.ie.capstone.menu.dto.response

import jnu.ie.capstone.menu.model.entity.Option

data class OptionResponse(
    val id: Long,
    val price: Long,
    val name: String
) {
    companion object {
        fun from(option: Option): OptionResponse {
            return OptionResponse(
                id = option.id,
                name = option.name.value,
                price = option.price.value
            )
        }
    }
}