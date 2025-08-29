package jnu.ie.capstone.common.security.dto

import jnu.ie.capstone.member.dto.MemberInfo
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.core.user.OAuth2User

data class KioskUserDetails(
    val memberInfo: MemberInfo,
    private val attributes: Map<String, Any>? = null,
    private val userNameAttributeName: String? = null
) : UserDetails, OAuth2User {

    companion object {
        private const val ROLE_PREFIX = "ROLE_"
    }

    override fun getUsername(): String = memberInfo.email.value
    override fun getPassword(): String = "Social"
    override fun getAuthorities() = listOf(SimpleGrantedAuthority(ROLE_PREFIX + memberInfo.role))
    override fun isAccountNonLocked() = super.isAccountNonLocked()
    override fun isAccountNonExpired() = super.isAccountNonExpired()
    override fun isCredentialsNonExpired() = super.isCredentialsNonExpired()
    override fun isEnabled() = super.isEnabled()
    override fun getName() = attributes?.get(userNameAttributeName)?.toString()
    override fun getAttributes() = this.attributes

}
