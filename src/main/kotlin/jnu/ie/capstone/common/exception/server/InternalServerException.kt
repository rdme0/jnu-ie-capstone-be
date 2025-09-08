package jnu.ie.capstone.common.exception.server

import jnu.ie.capstone.common.exception.BusinessException
import jnu.ie.capstone.common.exception.enums.ErrorCode

class InternalServerException(override val cause: Throwable) :
    BusinessException(ErrorCode.INTERNAL_SERVER, cause)