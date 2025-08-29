package jnu.ie.capstone.member.repository

import jnu.ie.capstone.member.model.entity.Member
import jnu.ie.capstone.member.model.vo.Email
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MemberRepository : JpaRepository<Member, Long> {
    fun findByEmail(email: Email): Member?
}