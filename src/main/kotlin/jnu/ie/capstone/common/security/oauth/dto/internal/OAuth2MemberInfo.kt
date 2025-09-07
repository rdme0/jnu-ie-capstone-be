package jnu.ie.capstone.common.security.oauth.dto.internal

import jnu.ie.capstone.common.exception.server.InternalServerException
import jnu.ie.capstone.member.model.entity.Oauth2Provider
import jnu.ie.capstone.member.model.vo.Email

data class OAuth2MemberInfo(
    val provider: Oauth2Provider,
    val email: Email
) {
    companion object {
        fun of(registrationId: String, attributes: Map<String, Any>): OAuth2MemberInfo {
            return when (registrationId) {
                "kakao" -> ofKakao(attributes)
                else -> throw InternalServerException(IllegalStateException("예상치 못한 registrationId -> $registrationId"))
            }
        }

        private fun ofKakao(attributes: Map<String, Any>): OAuth2MemberInfo {
            val account = attributes["kakao_account"] as? Map<*, *>
                ?: throw InternalServerException(IllegalStateException("kakao_account가 null일 수 없습니다."))

            val value = account["email"] as? String
                ?: throw InternalServerException(IllegalStateException("email이 null일 수 없습니다."))

            return OAuth2MemberInfo(provider = Oauth2Provider.KAKAO, email = Email(value))
        }
    }
}