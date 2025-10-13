package jnu.ie.capstone.menu.dto.request.interfaces

import jnu.ie.capstone.menu.dto.internal.MenuDTO

interface MenuRequest {
    val menus: List<MenuDTO>
}