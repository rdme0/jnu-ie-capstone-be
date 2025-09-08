package jnu.ie.capstone.member.service

import jnu.ie.capstone.common.security.oauth.dto.internal.OAuth2MemberInfo
import jnu.ie.capstone.member.dto.MemberInfo
import jnu.ie.capstone.member.model.entity.Member
import jnu.ie.capstone.member.model.vo.Email
import jnu.ie.capstone.member.service.internal.MemberDataService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemberCoordinateService(
    private val dataService: MemberDataService
) {
    @Transactional
    fun getOrSave(oAuth2MemberInfo: OAuth2MemberInfo): MemberInfo {
        val member = getEntityByEmail(oAuth2MemberInfo.email)
            ?: dataService.save(
                Member.builder()
                    .provider(oAuth2MemberInfo.provider)
                    .email(oAuth2MemberInfo.email)
                    .build()
            )

        return MemberInfo.from(member)
    }

    private fun getEntityByEmail(email: Email): Member? = dataService.get(email)

    @Transactional(readOnly = true)
    fun get(id: Long): MemberInfo? {
        val member = dataService.get(id) ?: return null
        return MemberInfo.from(member)
    }
}