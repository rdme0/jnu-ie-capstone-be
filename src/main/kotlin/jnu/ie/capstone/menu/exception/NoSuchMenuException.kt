package jnu.ie.capstone.menu.exception

import jnu.ie.capstone.common.exception.client.ClientException
import jnu.ie.capstone.common.exception.enums.ErrorCode

class NoSuchMenuException : ClientException(ErrorCode.NO_SUCH_MENU) {
    override val message: String
        get() = String.format(errorCode.message)
}