package jnu.ie.capstone.common.security.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jnu.ie.capstone.common.security.config.UriSecurityConfig
import jnu.ie.capstone.common.util.CookieUtil.addCookie
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.web.filter.OncePerRequestFilter

@Component
class RedirectParamFilter(
    private val config: UriSecurityConfig
) : OncePerRequestFilter() {

    companion object {
        private const val OAUTH2_AUTHORIZATION_PATH = "/oauth2/authorization"
        const val REDIRECT_ORIGIN_PARAM = "redirect"
        const val REDIRECT_ORIGIN_KEY = "OAUTH2_REDIRECT_ORIGIN"
        const val OAUTH2_COOKIE_MAX_AGE = 600
    }

    private val matcher = AntPathMatcher()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (request.servletPath.startsWith(OAUTH2_AUTHORIZATION_PATH)) {
            val redirectUri: String? = request.getParameter(REDIRECT_ORIGIN_PARAM)

            if (isAllowed(redirectUri)) {
                addCookie(
                    request = request,
                    response = response,
                    value = redirectUri,
                    key = REDIRECT_ORIGIN_KEY,
                    maxAge = OAUTH2_COOKIE_MAX_AGE
                )
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun isAllowed(uri: String?): Boolean {
        if (uri.isNullOrBlank()) return false

        return config.allowedFrontEndOrigins.stream()
            .anyMatch { pattern -> matcher.match(pattern, uri) }
    }
}