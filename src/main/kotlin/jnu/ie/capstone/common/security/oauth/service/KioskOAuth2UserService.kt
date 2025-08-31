package jnu.ie.capstone.common.security.oauth.service

import jnu.ie.capstone.common.security.dto.KioskUserDetails
import jnu.ie.capstone.common.security.oauth.dto.internal.OAuth2MemberInfo
import jnu.ie.capstone.member.service.MemberCoordinateService
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KioskOAuth2UserService(
    private val memberService: MemberCoordinateService
) : DefaultOAuth2UserService() {

    @Transactional
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2UserAttributes = super.loadUser(userRequest).attributes
        val registrationId = userRequest.clientRegistration.registrationId
        val userNameAttributeName = userRequest.clientRegistration
            .providerDetails.userInfoEndpoint.userNameAttributeName

        val oauth2MemberInfo = OAuth2MemberInfo.of(
            registrationId = registrationId,
            attributes = oAuth2UserAttributes
        )

        return KioskUserDetails(
            memberInfo = memberService.getOrSave(oauth2MemberInfo),
            attributes = oAuth2UserAttributes,
            userNameAttributeName = userNameAttributeName
        )
    }

}