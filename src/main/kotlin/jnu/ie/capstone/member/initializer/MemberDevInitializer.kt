package jnu.ie.capstone.member.initializer

import jnu.ie.capstone.common.security.util.JwtUtil
import jnu.ie.capstone.member.constant.MemberConstant.TEST_EMAIL
import jnu.ie.capstone.member.dto.MemberInfo
import jnu.ie.capstone.member.model.entity.Member
import jnu.ie.capstone.member.model.vo.Oauth2Provider
import jnu.ie.capstone.member.repository.MemberRepository
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(1)
@Profile("dev")
class MemberDevInitializer(
    private val memberRepository: MemberRepository,
    private val jwtUtil: JwtUtil
) {

    private companion object {
        val logger = mu.KotlinLogging.logger {}
    }

    @EventListener(ApplicationReadyEvent::class)
    fun init() {
        val member = memberRepository.findByEmail(TEST_EMAIL)
            ?: run {
                val testMember = Member.builder()
                    .provider(Oauth2Provider.KAKAO)
                    .email(TEST_EMAIL)
                    .build()

                memberRepository.save(testMember)
            }

        val accessToken = MemberInfo.from(member).let { jwtUtil.generateToken(it) }

        logger.info { "테스트 member accessToken -> $accessToken" }
    }

}