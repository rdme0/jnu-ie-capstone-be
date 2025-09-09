package jnu.ie.capstone.member.initializer

import jnu.ie.capstone.member.model.entity.Member
import jnu.ie.capstone.member.model.entity.Oauth2Provider
import jnu.ie.capstone.member.model.vo.Email
import jnu.ie.capstone.member.repository.MemberRepository
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
@Profile("dev")
class MemberDevInitializer(
    private val memberRepository: MemberRepository
) {
    private companion object {
        private val TEST_EMAIL = Email("Test@Test.com")
    }

    @EventListener(ApplicationReadyEvent::class)
    fun init() {
        memberRepository.findByEmail(TEST_EMAIL)
            ?: run {
                val testMember = Member.builder()
                    .provider(Oauth2Provider.KAKAO)
                    .email(TEST_EMAIL)
                    .build()

                memberRepository.save(testMember)
            }
    }

}