package jnu.ie.capstone.member.service.internal

import jnu.ie.capstone.member.model.entity.Member
import jnu.ie.capstone.member.model.vo.Email
import jnu.ie.capstone.member.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemberDataService(
    private val repository: MemberRepository
) {
    @Transactional
    fun save(member: Member) = repository.save(member)

    @Transactional(readOnly = true)
    fun get(email: Email) = repository.findByEmail(email)
}