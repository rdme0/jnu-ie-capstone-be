package jnu.ie.capstone.store.initializer

import jnu.ie.capstone.common.exception.server.InternalServerException
import jnu.ie.capstone.member.service.MemberCoordinateService
import jnu.ie.capstone.store.model.entity.Store
import jnu.ie.capstone.store.model.vo.StoreName
import jnu.ie.capstone.store.repository.StoreRepository
import mu.KotlinLogging
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Profile("dev")
@Order(2)
@Component
class StoreDevInitializer(
    private val memberCoordinateService: MemberCoordinateService,
    private val storeRepository: StoreRepository
) {
    private companion object {
        private val logger = KotlinLogging.logger {}
        const val STORE_COUNT = 10
    }

    @Transactional
    @EventListener(ApplicationReadyEvent::class)
    fun init() {
        val owner = memberCoordinateService.getEntity(1L)
            ?: throw InternalServerException(IllegalStateException("MASTER MEMBER를 찾을 수 없습니다."))

        if (storeRepository.count() < 1) {
            logger.info { "테스트 store ${STORE_COUNT}개 저장 중" }

            val stores = (1..STORE_COUNT).map {
                Store.builder()
                    .owner(owner)
                    .name(StoreName("테스트 스토어 $it"))
                    .build()
            }

            storeRepository.saveAll(stores)

            logger.info { "테스트 스토어 ${STORE_COUNT}개 저장 성공!" }
        }
    }
}