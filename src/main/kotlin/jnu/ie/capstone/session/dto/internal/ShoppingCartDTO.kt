package jnu.ie.capstone.session.dto.internal

data class ShoppingCartDTO(
    val menus: MutableList<ShoppingCartMenuDTO>
)

data class ShoppingCartMenuDTO(
    val id: Long,
    val name: String,
    val price: Long,
    val options: MutableList<ShoppingCartOptionDTO>
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
}

data class ShoppingCartOptionDTO(
    val id: Long,
    val name: String,
    val price: Long
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
}

