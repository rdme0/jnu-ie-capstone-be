package jnu.ie.capstone.gemini.dto.client.internal

import jnu.ie.capstone.menu.dto.internal.MenuInternalDTO
import jnu.ie.capstone.session.dto.internal.ShoppingCartDTO
import jnu.ie.capstone.session.enums.SessionState

sealed interface Context {
    companion object {
        val DIVIDER = "\n" + "=".repeat(20) + "\n"
    }

    data class MenuSelectionContext(
        val menus: List<MenuInternalDTO>,
        val shoppingCart: ShoppingCartDTO,
        val nowState: SessionState = SessionState.MENU_SELECTION
    ) : Context {
        override fun toString(): String {
            val menusText = menus
                .joinToString("\n" + "_".repeat(20) + "\n") { it.toString() }

            val shoppingCartText = "장바구니 : [\n${shoppingCart}\n]"

            return menusText + DIVIDER + shoppingCartText + DIVIDER + "현재 상태 : " + nowState
        }
    }

    data class CartConfirmationContext(
        val shoppingCart: ShoppingCartDTO,
        val nowState: SessionState = SessionState.CART_CONFIRMATION,
    ) : Context {
        override fun toString(): String {
            val shoppingCartText = "장바구니 : [\n${shoppingCart}\n]"

            return shoppingCartText + DIVIDER + "현재 상태 : " + nowState
        }
    }

    data class PaymentConfirmationContext(
        val shoppingCart: ShoppingCartDTO,
        val nowState: SessionState = SessionState.PAYMENT_CONFIRMATION,
    ) : Context {
        override fun toString(): String {
            val shoppingCartText = "장바구니 : [\n${shoppingCart}\n]"

            return shoppingCartText + DIVIDER + "현재 상태 : " + nowState
        }
    }

    object NoContext : Context {
        override fun toString() = "NoContext"
    }
}