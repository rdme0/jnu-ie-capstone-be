package jnu.ie.capstone.menu.service

import jnu.ie.capstone.common.exception.server.InternalServerException
import jnu.ie.capstone.gemini.client.GeminiClient
import jnu.ie.capstone.member.dto.MemberInfo
import jnu.ie.capstone.menu.dto.internal.MenuDTO
import jnu.ie.capstone.menu.dto.request.CreateMenuRequest
import jnu.ie.capstone.menu.dto.request.UpdateMenuRequest
import jnu.ie.capstone.menu.dto.request.interfaces.MenuRequest
import jnu.ie.capstone.menu.dto.response.MenuResponse
import jnu.ie.capstone.menu.exception.NoSuchMenuException
import jnu.ie.capstone.menu.model.entity.Menu
import jnu.ie.capstone.menu.model.entity.Option
import jnu.ie.capstone.menu.service.internal.MenuDataService
import jnu.ie.capstone.menu.service.internal.OptionDataService
import jnu.ie.capstone.store.exception.NoSuchStoreException
import jnu.ie.capstone.store.model.entity.Store
import jnu.ie.capstone.store.service.StoreService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MenuCoordinateService(
    private val geminiClient: GeminiClient,
    private val menuDataService: MenuDataService,
    private val optionDataService: OptionDataService,
    private val storeService: StoreService
) {

    @Transactional
    suspend fun save(storeId: Long, memberInfo: MemberInfo, request: CreateMenuRequest) {
        val store = storeService.get(storeId, memberInfo.id) ?: throw NoSuchStoreException()

        val menus: List<Pair<Menu, List<Option>?>> = assembleMenuWithOptions(
            request = request,
            store = store,
            ownerInfo = memberInfo
        )

        menuDataService.save(menus.map { it.first })

        val options = menus
            .flatMap { it.second ?: emptyList() }
            .takeIf { it.isNotEmpty() }

        if (options.isNullOrEmpty()) return

        optionDataService.save(options)
    }

    @Transactional(readOnly = true)
    fun get(storeId: Long, memberInfo: MemberInfo, pageable: Pageable): Page<MenuResponse> {
        val store = storeService.get(storeId, memberInfo.id) ?: throw NoSuchStoreException()

        val menus: Page<Menu> = menuDataService.getPageBy(store, memberInfo.id, pageable)

        val menuIds = menus.content.map { it.id }

        val optionsByMenuId: Map<Long, List<Option>> = optionDataService.getAllBy(menuIds)
            .groupBy { it.menu.id }

        return menus.map {
            MenuResponse.from(it, optionsByMenuId[it.id] ?: emptyList())
        }
    }

    @Transactional
    suspend fun update(storeId: Long, memberInfo: MemberInfo, request: UpdateMenuRequest) {
        val store = storeService.get(storeId, memberInfo.id) ?: throw NoSuchStoreException()
        val menus: List<Pair<Menu, List<Option>?>> = assembleMenuWithOptions(
            request = request,
            store = store,
            ownerInfo = memberInfo
        )

        val ids = menuDataService.getListBy(store, memberInfo.id).map { it.id }

        menuDataService.overwrite(ids, menus.map { it.first })

        val options = menus.map { it.second ?: emptyList() }.flatten().ifEmpty { return }
        
        //todo: overwrite dirty checking 방식으로 변경
        optionDataService.overwrite(menuIdsOfOptions = ids, options)
    }

    private suspend fun assembleMenuWithOptions(
        request: MenuRequest,
        store: Store,
        ownerInfo: MemberInfo
    ): List<Pair<Menu, List<Option>?>> {
        return when (request) {
            is CreateMenuRequest -> {
                request.menus.map { createDTO ->
                    val menuEmbeddings = geminiClient.getEmbedding(createDTO.name.value)
                        .first().values().get().toFloatArray()

                    buildMenuAndOptions(store, createDTO, menuEmbeddings)
                }
            }

            is UpdateMenuRequest -> {
                request.menus.map { updateDTO ->
                    val previousEntity = menuDataService.getBy(store, ownerInfo.id, updateDTO.id)
                        ?: throw NoSuchMenuException()

                    val menuEmbeddings = if (previousEntity.name.value != updateDTO.name.value) {
                        geminiClient.getEmbedding(updateDTO.name.value)
                            .first().values().get().toFloatArray()
                    } else {
                        previousEntity.embedding
                    }

                    buildMenuAndOptions(store, updateDTO, menuEmbeddings)
                }
            }

            else -> {
                throw InternalServerException(cause = IllegalStateException("알 수 없는 타입 -> ${request::class.simpleName}"))
            }
        }
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