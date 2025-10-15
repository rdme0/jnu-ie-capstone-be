package jnu.ie.capstone.menu.dto.internal

import jnu.ie.capstone.menu.model.entity.Menu
import jnu.ie.capstone.menu.model.entity.Option

data class MenuInternalDTO(
    val id: Long,
    val name: String,
    val price: Long,
    val options: List<OptionInternalDTO>? = null
) {
    companion object {
        fun from(menu: Menu, options: List<Option>? = null): MenuInternalDTO {
            return MenuInternalDTO(
                id = menu.id,
                name = menu.name.value,
                price = menu.price.value,
                options = options?.map { OptionInternalDTO.from(it) }
            )
        }
    }

    override fun toString(): String {

        val optionsString = options
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString("\n\n") { it.toString() }
            ?: "없음"

        return """
            메뉴 id : $id
            메뉴 이름 : $name
            가격 : ${price}원
            선택 가능한 옵션 : [
                $optionsString
            ]""".trimIndent()
    }
}