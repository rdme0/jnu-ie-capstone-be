package jnu.ie.capstone.common.security.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jnu.ie.capstone.common.security.exception.handler.Rest401Handler
import jnu.ie.capstone.common.security.helper.JwtAuthHelper
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val helper: JwtAuthHelper,
    private val rest401Handler: Rest401Handler
) : OncePerRequestFilter() {

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return path.startsWith("/oauth2/authorization")
            .or(path.startsWith("/login/oauth2/code"))
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
            SecurityContextHolder.clearContext()
            throw e
        }
    }
}