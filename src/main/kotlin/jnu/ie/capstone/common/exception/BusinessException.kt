package jnu.ie.capstone.common.exception

import jnu.ie.capstone.common.exception.enums.ErrorCode
import org.springframework.core.NestedRuntimeException

abstract class BusinessException(
    errorCode: ErrorCode,
    override val cause: Throwable? = null
) : NestedRuntimeException(errorCode.message, cause)