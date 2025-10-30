package jnu.ie.capstone.menu.initializer

import com.google.genai.errors.ServerException
import jnu.ie.capstone.common.exception.server.InternalServerException
import jnu.ie.capstone.gemini.constant.enums.GeminiModel
import jnu.ie.capstone.menu.model.entity.Menu
import jnu.ie.capstone.menu.model.entity.Option
import jnu.ie.capstone.menu.model.vo.MenuName
import jnu.ie.capstone.menu.model.vo.OptionName
import jnu.ie.capstone.menu.model.vo.Price
import jnu.ie.capstone.menu.repository.MenuRepository
import jnu.ie.capstone.menu.repository.OptionRepository
import jnu.ie.capstone.menu.util.MenuUtil
import jnu.ie.capstone.store.repository.StoreRepository
import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import kotlin.jvm.optionals.getOrNull
import kotlin.random.Random

@Component
@Order(3)
@Profile("prod")
class MenuProdInitializer(
    private val menuRepository: MenuRepository,
    private val optionRepository: OptionRepository,
    private val storeRepository: StoreRepository,
    private val util: MenuUtil
) {

    private companion object {
        val logger = KotlinLogging.logger {}
        val MENUS = listOf("아이스 아메리카노", "아이스 카페라떼", "아이스 카푸치노", "아이스 카페모카", "아이스티")
        val MENU_COUNT = MENUS.size
        val OPTIONS = listOf("샷 추가", "시럽 추가", "디카페인", "설탕 추가", "아이스크림 추가")
        val OPTION_COUNT = OPTIONS.size
    }

    @Transactional
    @EventListener(ApplicationReadyEvent::class)
    suspend fun init() {

        val store = storeRepository.findById(1L).getOrNull()
            ?: throw InternalServerException(IllegalStateException("스토어를 찾을 수 없습니다."))

        if (menuRepository.count() < 1) {
            logger.info { "테스트 menu ${MENU_COUNT}개 저장 중" }

            val menus = (1..MENU_COUNT).map {

                val name = MENUS[it % MENUS.size]

                val embeddings = try {
                    util.embedVector(name, GeminiModel.GEMINI_EMBEDDING_001)
                } catch (_: ServerException) {
                    util.embedVector(name, GeminiModel.TEXT_EMBEDDING_004)
                }

                delay(100)

                Menu.builder()
                    .name(MenuName(name))
                    .embedding(embeddings)
                    .store(store)
                    .price(Price(Random.nextLong(20000)))
                    .build()
            }

            menuRepository.saveAll(menus)

            logger.info { "테스트 menu ${MENU_COUNT}개 저장 성공!" }

            logger.info { "테스트 option ${MENU_COUNT * OPTION_COUNT}개 저장 중" }

            val options = menus.map { menu ->
                (1..OPTION_COUNT).map { index ->
                    Option.builder()
                        .name(OptionName(OPTIONS[index % OPTIONS.size]))
                        .menu(menu)
                        .price(Price(Random.nextLong(3000)))
                        .build()
                }
            }.flatten()

            optionRepository.saveAll(options)

            logger.info { "테스트 option ${MENU_COUNT * OPTION_COUNT}개 저장 성공!" }
        }
    }
}