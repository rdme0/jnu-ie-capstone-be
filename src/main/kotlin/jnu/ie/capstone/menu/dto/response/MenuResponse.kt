package jnu.ie.capstone.menu.dto.response

import jnu.ie.capstone.menu.model.entity.Menu

data class MenuResponse(
    val id: Long,
    val name: String,
    val price: Long
) {
    companion object {
        fun from(menu: Menu): MenuResponse {
            return MenuResponse(id = menu.id, name = menu.name.value, price = menu.price.value)
        }
    }
}