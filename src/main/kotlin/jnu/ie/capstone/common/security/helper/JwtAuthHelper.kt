package jnu.ie.capstone.common.security.helper

import jnu.ie.capstone.common.exception.client.UnauthorizedException
import jnu.ie.capstone.common.exception.server.InternalServerException
import jnu.ie.capstone.common.security.dto.KioskUserDetails
import jnu.ie.capstone.common.security.util.JwtUtil
import jnu.ie.capstone.member.service.MemberCoordinateService
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

@Component
class JwtAuthHelper(
    private val jwtUtil: JwtUtil,
    private val memberService: MemberCoordinateService
) {

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }

    fun authenticate(authHeader: String?, isBearer: Boolean = true): Authentication {
        authHeader ?: throw UnauthorizedException()

        val token = if (isBearer) {
            if (!authHeader.startsWith(BEARER_PREFIX)) throw UnauthorizedException()

            authHeader.removePrefix(BEARER_PREFIX)
        } else {
            authHeader
        }

        if (!jwtUtil.validateToken(token))
            throw UnauthorizedException()

        val memberId = jwtUtil.extractId(token) ?: throw InternalServerException(
            cause = IllegalStateException("토큰이 유효하지만 id 추출 실패")
        )

        val memberInfo = memberService.get(memberId) ?: throw InternalServerException(
            cause = IllegalStateException("토큰이 유효하지만 DB에 해당 회원 정보가 없음")
        )

        val kioskUserDetails = KioskUserDetails(memberInfo = memberInfo)

        return UsernamePasswordAuthenticationToken(
            kioskUserDetails,
            null,
            kioskUserDetails.authorities
        )

    }

}