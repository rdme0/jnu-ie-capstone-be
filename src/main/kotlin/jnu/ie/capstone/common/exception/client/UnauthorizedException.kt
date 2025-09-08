package jnu.ie.capstone.common.exception.client

import jnu.ie.capstone.common.exception.enums.ErrorCode
import org.springframework.security.core.AuthenticationException

class UnauthorizedException : AuthenticationException(ErrorCode.UNAUTHORIZED.message)