package jnu.ie.capstone.gemini.dto.client.internal

import jnu.ie.capstone.menu.dto.internal.MenuInternalDTO
import jnu.ie.capstone.session.dto.internal.ShoppingCartDTO

sealed interface Context {
    data class MenuAndShoppingCart(
        val menus: List<MenuInternalDTO>,
        val shoppingCart: ShoppingCartDTO
    ) : Context {
        override fun toString(): String {
            val menusText =  menus
                .joinToString("\n" + "_".repeat(20) + "\n") { it.toString() }

            val divider = "\n" + "=".repeat(20) + "\n"

            val shoppingCartText = "장바구니 : [\n${shoppingCart}\n]"

            return menusText + divider + shoppingCartText
        }
    }
}