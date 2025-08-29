package jnu.ie.capstone.common.security.oauth.dto.internal

data class OAuth2UserInfo(
    val registrationId: String,
    val attributes: Map<String, Any>
)
