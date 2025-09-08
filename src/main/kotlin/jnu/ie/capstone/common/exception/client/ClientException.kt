package jnu.ie.capstone.common.exception.client

import jnu.ie.capstone.common.exception.BusinessException
import jnu.ie.capstone.common.exception.enums.ErrorCode

abstract class ClientException(errorCode: ErrorCode) : BusinessException(errorCode)