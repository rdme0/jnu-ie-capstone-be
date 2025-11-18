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
    YOU_ARE_NOT_STORE_OWNER(Domain.STORE, HttpStatus.FORBIDDEN, 1, "당신은 스토어 주인이 아닙니다."),

    NO_SUCH_MENU(Domain.MENU, HttpStatus.NOT_FOUND, 1, "해당 메뉴는 없는 메뉴 입니다."),
    NO_SUCH_OPTION(Domain.MENU, HttpStatus.NOT_FOUND, 2, "해당 옵션은 없는 옵션 입니다."),

    OPTION_SIZE_MISMATCH(Domain.MENU, HttpStatus.BAD_REQUEST, 1, "요청하신 옵션 개수가 실제 옵션 개수와 다릅니다.");

    fun getCode() = "${domain.name}_${status.value()}_%03d".format(number)
}