package jnu.ie.capstone.menu.exception

import jnu.ie.capstone.common.exception.client.ClientException
import jnu.ie.capstone.common.exception.enums.ErrorCode

class OptionSizeMismatchException : ClientException(ErrorCode.OPTION_SIZE_MISMATCH) {
    override val message: String
        get() = String.format(errorCode.message)
}