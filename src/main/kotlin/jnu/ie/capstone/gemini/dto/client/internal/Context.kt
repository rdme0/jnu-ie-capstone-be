package jnu.ie.capstone.gemini.dto.client.internal

import jnu.ie.capstone.menu.dto.internal.MenuInternalDTO

sealed interface Context {
    data class MenuContext(val menus: List<MenuInternalDTO>) : Context {
        override fun toString(): String {
            return menus.joinToString("\n\n\n") { menu -> menu.toString() }
        }
    }
}