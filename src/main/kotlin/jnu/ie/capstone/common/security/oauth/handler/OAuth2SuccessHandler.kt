package jnu.ie.capstone.common.security.oauth.handler

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jnu.ie.capstone.common.security.dto.KioskUserDetails
import jnu.ie.capstone.common.security.util.JwtUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder


@Component
class OAuth2SuccessHandler(
    @param:Value("\${auth.success-uri}")
    val successUri : String,
    private val jwtUtil: JwtUtil
) : AuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val userDetails = authentication.principal as KioskUserDetails
        val accessToken: String = jwtUtil.generateToken(userDetails.memberInfo)
        val redirectUrl = UriComponentsBuilder.fromUriString(successUri)
            .queryParam("accessToken", accessToken)
            .build().toUriString()

        response.sendRedirect(redirectUrl)
    }

}