package jnu.ie.capstone.menu.service

import com.google.genai.errors.ServerException
import jnu.ie.capstone.gemini.constant.enums.GeminiModel
import jnu.ie.capstone.member.dto.MemberInfo
import jnu.ie.capstone.menu.constant.MenuConstant
import jnu.ie.capstone.menu.dto.internal.MenuInternalDTO
import jnu.ie.capstone.menu.dto.internal.interfaces.MenuDTO
import jnu.ie.capstone.menu.dto.request.CreateMenuRequest
import jnu.ie.capstone.menu.dto.request.CreateOptionRequest
import jnu.ie.capstone.menu.dto.request.UpdateMenuRequest
import jnu.ie.capstone.menu.dto.request.UpdateOptionRequest
import jnu.ie.capstone.menu.dto.response.MenuResponse
import jnu.ie.capstone.menu.dto.response.OptionResponse
import jnu.ie.capstone.menu.exception.NoSuchMenuException
import jnu.ie.capstone.menu.exception.NoSuchOptionException
import jnu.ie.capstone.menu.model.entity.Menu
import jnu.ie.capstone.menu.model.entity.Option
import jnu.ie.capstone.menu.service.internal.MenuDataService
import jnu.ie.capstone.menu.service.internal.OptionDataService
import jnu.ie.capstone.menu.util.MenuUtil
import jnu.ie.capstone.store.annotation.AssertStoreOwner
import jnu.ie.capstone.store.exception.NoSuchStoreException
import jnu.ie.capstone.store.model.entity.Store
import jnu.ie.capstone.store.service.StoreService
import jnu.ie.capstone.store.service.internal.StoreDataService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Suppress("UNUSED_PARAMETER")
class MenuCoordinateService(
    private val menuDataService: MenuDataService,
    private val optionDataService: OptionDataService,
    private val storeDataService: StoreDataService,
    private val util: MenuUtil
) {

    private companion object {
        val EMBEDDING_PLAN = EmbeddingPlan(
            planA = GeminiModel.GEMINI_EMBEDDING_001,
            planB = GeminiModel.TEXT_EMBEDDING_004
        )
    }

    @Transactional
    @AssertStoreOwner(memberInfo = "#ownerInfo", storeId = "#storeId")
    fun createMenu(storeId: Long, ownerInfo: MemberInfo, request: CreateMenuRequest) {
        val store = storeDataService.getBy(storeId, ownerInfo.id) ?: throw NoSuchStoreException()

        val menus: List<Pair<Menu, List<Option>?>> = assembleMenuWithOptions(
            request = request,
            store = store
        )

        menuDataService.save(menus.map { it.first })

        val options = menus
            .flatMap { it.second ?: emptyList() }
            .takeIf { it.isNotEmpty() }

        if (options.isNullOrEmpty()) return

        optionDataService.save(options)
    }

    @Transactional
    @AssertStoreOwner(memberInfo = "#ownerInfo", storeId = "#storeId")
    fun createOption(
        storeId: Long,
        menuId: Long,
        ownerInfo: MemberInfo,
        request: CreateOptionRequest
    ) {
        val menu = menuDataService.getBy(storeId, menuId) ?: throw NoSuchMenuException()

        val newOption = Option.builder()
            .menu(menu)
            .name(request.option.name)
            .price(request.option.price)
            .build()

        optionDataService.save(newOption)
    }

    @Transactional(readOnly = true)
    @AssertStoreOwner(memberInfo = "#ownerInfo", storeId = "#storeId")
    fun getMenuResponses(
        storeId: Long,
        ownerInfo: MemberInfo,
        pageable: Pageable
    ): Page<MenuResponse> {
        val menus: Page<Menu> = menuDataService.getPageBy(storeId, pageable)

        return menus.map { MenuResponse.from(it) }
    }

    @Transactional(readOnly = true)
    @AssertStoreOwner(memberInfo = "#ownerInfo", storeId = "#storeId")
    fun getOptionResponses(
        storeId: Long,
        menuId: Long,
        ownerInfo: MemberInfo,
        pageable: Pageable
    ): Page<OptionResponse> {
        val menu = menuDataService.getBy(storeId, menuId) ?: throw NoSuchMenuException()

        return optionDataService
            .getAllBy(menu.id, pageable)
            .map { OptionResponse.from(it) }
    }

    @Transactional
    @AssertStoreOwner(memberInfo = "#ownerInfo", storeId = "#storeId")
    fun updateMenu(
        storeId: Long,
        menuId: Long,
        ownerInfo: MemberInfo,
        request: UpdateMenuRequest
    ) {
        val oldMenu = menuDataService.getBy(storeId = storeId, id = menuId)
            ?: throw NoSuchMenuException()

        updateMenuEntity(oldMenu, request)
    }

    @Transactional
    @AssertStoreOwner(memberInfo = "#ownerInfo", storeId = "#storeId")
    fun updateOption(
        storeId: Long,
        menuId: Long,
        optionId: Long,
        ownerInfo: MemberInfo,
        request: UpdateOptionRequest
    ) {
        val menu = menuDataService.getBy(storeId, menuId) ?: throw NoSuchMenuException()
        val option = optionDataService.getBy(menu.id, optionId) ?: throw NoSuchOptionException()

        option.name = request.option.name
        option.price = request.option.price
    }

    @Transactional
    @AssertStoreOwner(memberInfo = "#ownerInfo", storeId = "#storeId")
    fun deleteOption(
        storeId: Long,
        menuId: Long,
        optionId: Long,
        ownerInfo: MemberInfo,
    ) {
        menuDataService.getBy(storeId, menuId) ?: throw NoSuchMenuException()
        optionDataService.deleteBy(optionId)
    }

    @Transactional
    @AssertStoreOwner(memberInfo = "#ownerInfo", storeId = "#storeId")
    fun deleteMenu(
        storeId: Long,
        menuId: Long,
        ownerInfo: MemberInfo,
    ) {
        val menu = menuDataService.getBy(storeId, menuId) ?: throw NoSuchMenuException()

        optionDataService.deleteAllBy(menuId = menu.id)
        menuDataService.deleteBy(menu.id)
    }

    @Transactional
    @AssertStoreOwner(memberInfo = "#ownerInfo", storeId = "#storeId")
    fun deleteAllMenuBy(
        storeId: Long,
        ownerInfo: MemberInfo
    ) {

        val menuIds: List<Long> = menuDataService
            .getListBy(storeId)
            .map(Menu::getId)

        optionDataService.deleteAllBy(menuIds)
        menuDataService.deleteByStoreId(storeId)
    }

    @Transactional(readOnly = true)
    @AssertStoreOwner(memberInfo = "#ownerInfo", storeId = "#storeId")
    fun getMenuInternal(
        storeId: Long,
        ownerInfo: MemberInfo,
        menuId: Long
    ): MenuInternalDTO {
        val menu = menuDataService.getBy(storeId, menuId) ?: throw NoSuchMenuException()
        val optionsByOneMenu = optionDataService.getAllBy(menu.id)

        return MenuInternalDTO.from(menu, optionsByOneMenu)
    }

    @Transactional(readOnly = true)
    @AssertStoreOwner(memberInfo = "#ownerInfo", storeId = "#storeId")
    fun getMenuInternalByOptionId(
        storeId: Long,
        ownerInfo: MemberInfo,
        optionId: Long
    ): MenuInternalDTO {
        val option = optionDataService.getBy(optionId) ?: throw NoSuchOptionException()
        val menu = option.menu

        if (menu.store.id != storeId) throw NoSuchMenuException()

        return MenuInternalDTO.from(menu, listOf(option))
    }

    @Transactional(readOnly = true)
    @AssertStoreOwner(memberInfo = "#ownerInfo", storeId = "#storeId")
    fun getMenuRelevant(
        text: String,
        storeId: Long,
        ownerInfo: MemberInfo,
    ): List<MenuInternalDTO> {

        val embedding = try {
            util.embedVector(text, EMBEDDING_PLAN.planA)
        } catch (_: ServerException) {
            util.embedVector(text, EMBEDDING_PLAN.planB)
        }

        val relevantMenus = menuDataService.getRelevantBy(
            storeId = storeId,
            embedding = embedding,
            limit = MenuConstant.RELEVANT_MENU_SIZE
        )

        val optionsByRelevantMenuId: Map<Long, List<Option>> = optionDataService
            .getAllBy(relevantMenus.map { it.id })
            .groupBy { it.menu.id }

        return relevantMenus.map {
            MenuInternalDTO.from(
                menu = it,
                options = optionsByRelevantMenuId[it.id] ?: emptyList()
            )
        }
    }

    private fun assembleMenuWithOptions(
        request: CreateMenuRequest,
        store: Store
    ): List<Pair<Menu, List<Option>?>> {
        return request.menus.map { createDTO ->
            val menuEmbeddings = try {
                util.embedVector(createDTO.name.value, EMBEDDING_PLAN.planA)
            } catch (_: ServerException) {
                util.embedVector(createDTO.name.value, EMBEDDING_PLAN.planB)
            }
            buildMenuAndOptions(store, createDTO, menuEmbeddings)
        }
    }

    private fun updateMenuEntity(
        oldMenu: Menu,
        request: UpdateMenuRequest
    ): Menu {
        if (oldMenu.name.value != request.name.value) {
            oldMenu.embedding = try {
                util.embedVector(request.name.value, EMBEDDING_PLAN.planA)
            } catch (_: ServerException) {
                util.embedVector(request.name.value, EMBEDDING_PLAN.planB)
            }
        }

        oldMenu.name = request.name
        oldMenu.price = request.price

        return oldMenu
    }

    private fun buildMenuAndOptions(
        store: Store,
        menuDTO: MenuDTO,
        embedding: FloatArray
    ): Pair<Menu, List<Option>?> {
        val menuEntity = Menu.builder()
            .store(store)
            .name(menuDTO.name)
            .price(menuDTO.price)
            .embedding(embedding)
            .build()

        val optionEntities = menuDTO.options?.map {
            Option.builder()
                .menu(menuEntity)
                .name(it.name)
                .price(it.price)
                .build()
        }

        return menuEntity to optionEntities
    }
}

private data class EmbeddingPlan(
    val planA: GeminiModel,
    val planB: GeminiModel
)