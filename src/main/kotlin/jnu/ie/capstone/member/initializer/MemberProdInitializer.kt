package jnu.ie.capstone.member.initializer

import jnu.ie.capstone.member.constant.MemberConstant.TEST_EMAIL
import jnu.ie.capstone.member.model.entity.Member
import jnu.ie.capstone.member.model.vo.Oauth2Provider
import jnu.ie.capstone.member.repository.MemberRepository
import mu.KotlinLogging
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Order(1)
@Profile("prod")
class MemberProdInitializer(
    private val memberRepository: MemberRepository,
) {

    private companion object {
        val logger = KotlinLogging.logger {}
    }

    @Transactional
    @EventListener(ApplicationReadyEvent::class)
    fun init() {
        memberRepository.findByEmail(TEST_EMAIL)
            ?: run {
                logger.info { "테스트 member 저장 중" }
                val testMember = Member.builder()
                    .provider(Oauth2Provider.KAKAO)
                    .email(TEST_EMAIL)
                    .build()

                memberRepository.save(testMember)

                logger.info { "테스트 member 저장 완료" }
            }
    }

}