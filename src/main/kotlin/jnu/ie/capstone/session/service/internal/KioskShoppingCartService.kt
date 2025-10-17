package jnu.ie.capstone.session.service.internal

import jnu.ie.capstone.common.function.not
import jnu.ie.capstone.gemini.dto.client.response.GeminiFunctionParams.AddItems
import jnu.ie.capstone.gemini.dto.client.response.GeminiFunctionParams.RemoveItems
import jnu.ie.capstone.member.dto.MemberInfo
import jnu.ie.capstone.menu.service.MenuCoordinateService
import jnu.ie.capstone.session.dto.internal.ShoppingCartDTO
import jnu.ie.capstone.session.dto.internal.ShoppingCartMenuDTO
import jnu.ie.capstone.session.dto.internal.ShoppingCartOptionDTO
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class KioskShoppingCartService(
    private val menuService: MenuCoordinateService
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    fun addMenu(
        storeId: Long,
        ownerInfo: MemberInfo,
        shoppingCart: ShoppingCartDTO,
        addItems: AddItems
    ) {
        addItems.orderItems.forEach { request ->
            val menuId = request.menuId
            val optionIds = request.optionIds

            when {
                menuId != null -> {
                    addMenuToShoppingCart(storeId, ownerInfo, menuId, optionIds, shoppingCart)
                }

                not(optionIds.isNullOrEmpty()) -> {
                    optionIds?.forEach { optionId ->
                        addOnlyOptionToShoppingCart(storeId, ownerInfo, optionId, shoppingCart)
                    }
                }

                else -> {
                    logger.warn { "잘못된 AI 응답 -> menuId와 optionIds가 모두 없음" }
                }
            }
        }
    }

    fun removeMenu(
        shoppingCart: ShoppingCartDTO,
        removeItems: RemoveItems
    ) {
        val menuIdsToRemove = removeItems.removeMenuIds
        val optionIdsToRemove = removeItems.removeOptionIds

        if (menuIdsToRemove.isNullOrEmpty() && optionIdsToRemove.isNullOrEmpty()) {
            logger.warn { "잘못된 AI 응답 -> menuIds와 optionIds가 모두 없음" }
            return
        }

        if (!menuIdsToRemove.isNullOrEmpty()) {
            val initialSize = shoppingCart.menus.size
            shoppingCart.menus.removeIf { menu -> menuIdsToRemove.contains(menu.id) }

            if (initialSize == shoppingCart.menus.size)
                logger.warn { "잘못된 AI 응답 -> 제거할 메뉴(ID:${menuIdsToRemove})를 찾지 못함" }
        }

        if (!optionIdsToRemove.isNullOrEmpty()) {
            var itemsRemoved = false

            shoppingCart.menus.forEach { menu ->
                val initialOptionsSize = menu.options.size
                menu.options.removeIf { option -> optionIdsToRemove.contains(option.id) }

                if (initialOptionsSize > menu.options.size) itemsRemoved = true
            }

            if (not(itemsRemoved))
                logger.warn { "잘못된 AI 응답 -> 제거할 옵션(ID:${optionIdsToRemove})을 찾지 못함" }
        }
    }

    private fun addMenuToShoppingCart(
        storeId: Long,
        ownerInfo: MemberInfo,
        requestMenuId: Long,
        requestOptionIds: List<Long>?,
        shoppingCart: ShoppingCartDTO
    ) {
        val menuFromDb = runCatching {
            menuService.getMenuInternal(storeId, ownerInfo, menuId = requestMenuId)
        }.getOrNull() ?: run {
            logger.warn { "잘못된 ai 응답 -> id가 ${requestMenuId}인 메뉴를 찾지 못함" }
            return
        }

        val newOptions = menuFromDb.options
            ?.filter { optionFromDb -> requestOptionIds?.contains(optionFromDb.id) ?: false }
            ?.map { ShoppingCartOptionDTO(it.id, it.name, it.price) }
            ?: emptyList()

        val newMenu = ShoppingCartMenuDTO(
            id = menuFromDb.id,
            name = menuFromDb.name,
            price = menuFromDb.price,
            options = newOptions.toMutableList()
        )

        shoppingCart.menus.add(newMenu)
    }

    private fun addOnlyOptionToShoppingCart(
        storeId: Long,
        ownerInfo: MemberInfo,
        requestOptionId: Long,
        shoppingCart: ShoppingCartDTO
    ) {
        val menuFromDb = runCatching {
            menuService.getMenuInternalByOptionId(storeId, ownerInfo, requestOptionId)
        }.getOrNull()
            ?: run {
                logger.warn { "잘못된 ai 응답 -> 옵션 id가 ${requestOptionId}인 메뉴를 찾지 못함" }
                return
            }

        val targetMenuInCart = shoppingCart.menus.find { it.id == menuFromDb.id }
            ?: run {
                logger.warn { "잘못된 ai 응답 -> 옵션(ID:${requestOptionId})의 부모 메뉴가 장바구니에 없어 요청을 무시함" }
                return
            }

        val newOption = menuFromDb.options?.first()
            ?: run {
                logger.warn { "잘못된 ai 응답 -> 옵션(ID:${requestOptionId})을 db에서 찾지 못함" }
                return
            }

        val newOptionInCart = ShoppingCartOptionDTO(newOption.id, newOption.name, newOption.price)

        targetMenuInCart.options.add(newOptionInCart)
    }
}
