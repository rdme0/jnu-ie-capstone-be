package jnu.ie.capstone.member.service

import jnu.ie.capstone.member.service.internal.MemberDataService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemberCoordinateService(
    private val dataService: MemberDataService
) {

    @Transactional(readOnly = true)
    fun get(dataService: MemberDataService)

}