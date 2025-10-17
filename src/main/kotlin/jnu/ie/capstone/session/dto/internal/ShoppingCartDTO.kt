package jnu.ie.capstone.session.dto.internal

import jnu.ie.capstone.common.util.TimeUtil
import java.time.ZonedDateTime

data class ShoppingCartDTO(
    val menus: MutableList<ShoppingCartMenuDTO>
) {

    override fun toString(): String {
        val menuString = if (menus.isEmpty()) "비었음"
        else menus.joinToString("\n" + "_".repeat(20) + "\n") { it.toString() }

        return menuString
    }

}

data class ShoppingCartMenuDTO(
    val id: Long,
    val name: String,
    val price: Long,
    val options: MutableList<ShoppingCartOptionDTO>,
    val createdAt: ZonedDateTime = TimeUtil.zonedDateTimeNow()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShoppingCartMenuDTO

        return id == other.id
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + price.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + options.hashCode()

        return result
    }

    override fun toString(): String {
        val optionsString = if (options.isEmpty()) "없음" else options.joinToString("\n\n")

        return "메뉴 id : $id\n메뉴 이름 : $name\n메뉴 가격 : $price 원\n장바구니 추가 일시 : ${
            TimeUtil.formatToSimpleString(
                createdAt
            )
        }\n선택한 옵션 : [\n$optionsString\n]"
    }
}

data class ShoppingCartOptionDTO(
    val id: Long,
    val name: String,
    val price: Long,
    val createdAt: ZonedDateTime = TimeUtil.zonedDateTimeNow()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShoppingCartOptionDTO

        return id == other.id
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + price.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    override fun toString(): String {
        return "옵션 id : $id\n옵션 이름 : $name\n옵션 가격 : $price 원\n장바구니 추가 일시 : ${
            TimeUtil.formatToSimpleString(
                createdAt
            )
        }"
    }
}

