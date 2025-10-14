package jnu.ie.capstone.menu.service

import com.google.genai.errors.ServerException
import jnu.ie.capstone.common.exception.server.InternalServerException
import jnu.ie.capstone.gemini.client.GeminiClient
import jnu.ie.capstone.gemini.constant.enums.GeminiModel
import jnu.ie.capstone.member.dto.MemberInfo
import jnu.ie.capstone.menu.constant.MenuConstant
import jnu.ie.capstone.menu.dto.internal.interfaces.MenuDTO
import jnu.ie.capstone.menu.dto.internal.MenuInternalDTO
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
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MenuCoordinateService(
    private val geminiClient: GeminiClient,
    private val menuDataService: MenuDataService,
    private val optionDataService: OptionDataService,
    private val storeService: StoreService,
) {

    private companion object {
        val EMBEDDING_PLAN = EmbeddingPlan(
            planA = GeminiModel.GEMINI_EMBEDDING_001,
            planB = GeminiModel.TEXT_EMBEDDING_004
        )
    }

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
            MenuInternalDTO.from(it, optionsByMenuId[it.id] ?: emptyList()).toResponse()
        }
    }

    @Transactional(readOnly = true)
    suspend fun getRelevant(
        text: String,
        storeId: Long,
        ownerInfo: MemberInfo,
    ): List<MenuInternalDTO> {
        val store = storeService.get(storeId, ownerInfo.id) ?: throw NoSuchStoreException()

        val embedding = try {
            embedVector(text, EMBEDDING_PLAN.planA)
        } catch (_: ServerException) {
            embedVector(text, EMBEDDING_PLAN.planB)
        }

        val relevantMenus = menuDataService.getRelevantBy(
            store = store,
            ownerId = ownerInfo.id,
            embedding = embedding,
            limit = MenuConstant.RELEVANT_MENU_SIZE
        )

        val optionsByRelevantMenuId: Map<Long, List<Option>> =
            optionDataService.getAllBy(relevantMenus.map { it.id })
                .groupBy { it.menu.id }

        return relevantMenus.map {
            MenuInternalDTO.from(
                menu = it,
                options = optionsByRelevantMenuId[it.id] ?: emptyList()
            )
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
                    val menuEmbeddings = try {
                        embedVector(createDTO.name.value, EMBEDDING_PLAN.planA)
                    } catch (_: ServerException) {
                        embedVector(createDTO.name.value, EMBEDDING_PLAN.planB)
                    }

                    buildMenuAndOptions(store, createDTO, menuEmbeddings)
                }
            }

            is UpdateMenuRequest -> {
                request.menus.map { updateDTO ->
                    val previousEntity = menuDataService.getBy(store, ownerInfo.id, updateDTO.id)
                        ?: throw NoSuchMenuException()

                    val menuEmbeddings = if (previousEntity.name.value != updateDTO.name.value) {
                        try {
                            embedVector(updateDTO.name.value, EMBEDDING_PLAN.planA)
                        } catch (e: ServerException) {
                            embedVector(updateDTO.name.value, EMBEDDING_PLAN.planB)
                        }
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

    //todo: jitter 추가
    @Retryable(
        value = [ServerException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1000, multiplier = 2.0)
    )
    private suspend fun embedVector(text: String, model: GeminiModel): FloatArray =
        geminiClient.getEmbedding(text, model).first().values().get().toFloatArray()
}

private data class EmbeddingPlan(
    val planA: GeminiModel,
    val planB: GeminiModel
)