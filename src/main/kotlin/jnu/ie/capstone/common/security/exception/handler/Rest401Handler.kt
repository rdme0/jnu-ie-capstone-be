package jnu.ie.capstone.common.security.exception.handler

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jnu.ie.capstone.common.dto.response.CommonResponse
import jnu.ie.capstone.common.exception.enums.ErrorCode
import jnu.ie.capstone.common.security.constants.SecurityConstants.AUTHORIZATION_HEADER
import mu.KotlinLogging
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

@Component
class Rest401Handler : AuthenticationEntryPoint {
    private val logger = KotlinLogging.logger {}

    companion object {
        private val mapper: ObjectMapper = ObjectMapper()
    }

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = "application/json"
        response.characterEncoding = "UTF-8"

        logger.warn {
            ">>> 401 (Unauthorized) - ${authException.message}, URI - ${request.requestURI}, authorization header - ${
                request.getHeader(
                    AUTHORIZATION_HEADER
                )
            }"
        }

        mapper.writeValue(
            response.writer,
            CommonResponse.ofFailure(ErrorCode.UNAUTHORIZED)
        )
    }
}