package jnu.ie.capstone.common.exception.enums

import jnu.ie.capstone.common.constant.enums.Domain
import org.springframework.http.HttpStatus

enum class ErrorCode(
    private val domain: Domain,
    val status: HttpStatus,
    private val number: Int,
    val message: String
) {
    INTERNAL_SERVER(Domain.COMMON, HttpStatus.INTERNAL_SERVER_ERROR, 1, "서버 내부 오류입니다."),
    INVALID_INPUT_VALUE(Domain.COMMON, HttpStatus.BAD_REQUEST, 1, "유효하지 않은 입력 값입니다."),
    BAD_DATA_SYNTAX(Domain.COMMON, HttpStatus.BAD_REQUEST, 2, "%s"),
    INVALID_PAGEABLE_FIELD(
        Domain.COMMON,
        HttpStatus.BAD_REQUEST,
        3,
        "페이징 가능한 필드가 아닙니다. -> %s = %s"
    ),
    UNAUTHORIZED(Domain.COMMON, HttpStatus.UNAUTHORIZED, 1, "인증되지 않은 사용자 입니다."),
    BAD_DATA_MEANING(Domain.COMMON, HttpStatus.UNPROCESSABLE_ENTITY, 1, "%s"),

    NO_SUCH_STORE(Domain.STORE, HttpStatus.NOT_FOUND, 1, "해당 스토어는 없는 스토어 입니다."),
    NO_SUCH_MENU(Domain.MENU, HttpStatus.NOT_FOUND, 1, "해당 메뉴는 없는 메뉴 입니다.");

    fun getCode() = "${domain.name}_${status.value()}_%03d".format(number)
}