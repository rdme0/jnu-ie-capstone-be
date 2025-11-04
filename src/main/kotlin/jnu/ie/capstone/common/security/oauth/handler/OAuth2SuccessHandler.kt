package jnu.ie.capstone.common.security.oauth.handler

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jnu.ie.capstone.common.security.config.UriSecurityConfig
import jnu.ie.capstone.common.security.dto.KioskUserDetails
import jnu.ie.capstone.common.security.filter.RedirectParamFilter
import jnu.ie.capstone.common.security.util.JwtUtil
import jnu.ie.capstone.common.util.CookieUtil.removeCookie
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import org.springframework.web.util.WebUtils


@Component
class OAuth2SuccessHandler(
    private val config: UriSecurityConfig,
    private val jwtUtil: JwtUtil
) : AuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val userDetails = authentication.principal as? KioskUserDetails
            ?: run {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                return
            }
        val accessToken: String = jwtUtil.generateToken(userDetails.memberInfo)

        val redirectOrigin: String = WebUtils
            .getCookie(request, RedirectParamFilter.REDIRECT_ORIGIN_KEY)
            ?.value
            ?: config.defaultRedirectOrigin

        removeCookie(
            request = request,
            response = response,
            key = RedirectParamFilter.REDIRECT_ORIGIN_KEY
        )

        val redirectUrl = UriComponentsBuilder.fromUriString(redirectOrigin)
            .path(config.successPath)
            .queryParam("accessToken", accessToken)
            .build()
            .toUriString()

        response.sendRedirect(redirectUrl)
    }

}