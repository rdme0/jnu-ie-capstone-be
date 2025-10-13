package jnu.ie.capstone.store.exception

import jnu.ie.capstone.common.exception.client.ClientException
import jnu.ie.capstone.common.exception.enums.ErrorCode

class NoSuchStoreException : ClientException(ErrorCode.NO_SUCH_STORE) {
    override val message: String
        get() = String.format(errorCode.message)
}