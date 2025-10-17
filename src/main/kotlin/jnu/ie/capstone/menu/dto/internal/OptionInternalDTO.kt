package jnu.ie.capstone.menu.dto.internal

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

    override fun toString(): String {
        return "옵션 id : $id\n옵션 이름 : $name\n옵션 가격 : $price 원"
    }
}