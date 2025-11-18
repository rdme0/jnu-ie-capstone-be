package jnu.ie.capstone.store.exception

import jnu.ie.capstone.common.exception.client.ClientException
import jnu.ie.capstone.common.exception.enums.ErrorCode

class YouAreNotStoreOwnerException : ClientException(ErrorCode.YOU_ARE_NOT_STORE_OWNER) {
    override val message: String
        get() = String.format(errorCode.message)
}