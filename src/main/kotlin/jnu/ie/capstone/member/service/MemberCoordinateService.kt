package jnu.ie.capstone.member.service

import jnu.ie.capstone.common.security.oauth.dto.internal.OAuth2MemberInfo
import jnu.ie.capstone.member.model.entity.Member
import jnu.ie.capstone.member.service.internal.MemberDataService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemberCoordinateService(
    private val dataService: MemberDataService
) {
    @Transactional(readOnly = true)
    fun getOrSave(oAuth2MemberInfo: OAuth2MemberInfo): Member {
        return dataService.get(oAuth2MemberInfo.email)
            ?: dataService.save(
                Member.builder()
                    .email(oAuth2MemberInfo.email)
                    .build()
            )
    }
}