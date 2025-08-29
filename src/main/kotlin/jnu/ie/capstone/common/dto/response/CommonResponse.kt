package jnu.ie.capstone.common.dto.response

import jnu.ie.capstone.common.exception.enums.ErrorCode

data class CommonResponse(
    private val isSuccess: Boolean,
    private val message: String,
    private val errorCode: String?
) {
    companion object {
        private const val SUCCESS = "success"

        fun ofSuccess(): CommonResponse = CommonResponse(true, SUCCESS, null)

        fun ofFailure(errorCode: ErrorCode): CommonResponse {
            return CommonResponse(false, errorCode.message, errorCode.getCode())
        }

        fun ofFailure(customErrorMessage: String, errorCode: ErrorCode): CommonResponse {
            return CommonResponse(false, customErrorMessage, errorCode.getCode())
        }
    }

}
