package jnu.ie.capstone.common.dto.response

import jnu.ie.capstone.common.exception.enums.ErrorCode

data class CommonResponse(
    val isSuccess: Boolean,
    val message: String,
    val errorCode: String?
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
