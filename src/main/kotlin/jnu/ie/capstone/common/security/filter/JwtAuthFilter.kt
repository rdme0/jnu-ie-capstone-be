package jnu.ie.capstone.common.security.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jnu.ie.capstone.common.constant.CommonConstants.CRITICAL_ERROR_MESSAGE
import jnu.ie.capstone.common.security.constants.SecurityConstants.AUTHORIZATION_HEADER
import jnu.ie.capstone.common.security.exception.handler.Rest401Handler
import jnu.ie.capstone.common.security.exception.handler.Rest500Handler
import jnu.ie.capstone.common.security.helper.JwtAuthHelper
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val helper: JwtAuthHelper,
    private val rest401Handler: Rest401Handler,
    private val rest500Handler: Rest500Handler
) : OncePerRequestFilter() {

    companion object {
        private val WHITELIST = setOf(
            "/h2-console/**",
            "/auth/success",
            "/error",
            "/favicon.ico",
            "/oauth2/authorization/**",
            "/login/**",
            "/websocket/voice/**"
        )
        private val pathMatcher: AntPathMatcher = AntPathMatcher()
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return WHITELIST.stream().anyMatch { pathMatcher.match(it, path) }
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val auth = helper.authenticate(request.getHeader(AUTHORIZATION_HEADER))
                    as UsernamePasswordAuthenticationToken
            auth.details = WebAuthenticationDetailsSource().buildDetails(request)
            SecurityContextHolder.getContext().authentication = auth
            filterChain.doFilter(request, response)
        } catch (e: AuthenticationException) {
            SecurityContextHolder.clearContext()
            rest401Handler.commence(request, response, e)
        } catch (e: Exception) {
            rest500Handler.commence(
                request,
                response,
                AuthenticationServiceException(CRITICAL_ERROR_MESSAGE.format("인증"), e)
            )
            SecurityContextHolder.clearContext()
        }
    }
}