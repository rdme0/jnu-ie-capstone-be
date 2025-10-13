package jnu.ie.capstone.common.exception.client;

import jnu.ie.capstone.common.exception.enums.ErrorCode;

class BadDataLengthException(
    private val fieldName: String?,
    private val minLength: Int,
    private val maxLength: Int
) : ClientException(ErrorCode.BAD_DATA_MEANING) {
    override val message: String
        get() {
            return String.format(
                errorCode.message,
                "${fieldName}의 길이는 ${minLength}자 부터 ${maxLength}자 까지 가능합니다."
            )
        }
}

