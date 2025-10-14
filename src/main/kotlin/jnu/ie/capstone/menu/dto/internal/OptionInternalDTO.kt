package jnu.ie.capstone.menu.dto.internal

import jnu.ie.capstone.menu.dto.response.OptionResponse
import jnu.ie.capstone.menu.model.entity.Option

data class OptionInternalDTO(
    val id: Long,
    val price: Long,
    val name: String
) {
    companion object {
        fun from(option: Option): OptionInternalDTO {
            return OptionInternalDTO(
                id = option.id,
                price = option.price.value,
                name = option.name.value
            )
        }
    }

    fun toResponse(): OptionResponse {
        return OptionResponse(
            id = id,
            price = price,
            name = name
        )
    }

    override fun toString(): String {
        return """
            옵션 id : $id
            옵션 이름 : $name
            옵션 가격 : $price
        """.trimIndent()
    }
}