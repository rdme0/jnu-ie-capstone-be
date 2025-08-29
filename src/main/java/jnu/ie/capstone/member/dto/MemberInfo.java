package jnu.ie.capstone.member.dto;

import jnu.ie.capstone.member.model.entity.Member;
import jnu.ie.capstone.member.model.entity.Role;
import jnu.ie.capstone.member.model.vo.Email;
import lombok.Builder;

@Builder
public record MemberInfo(Long id, Email email, Role role) {

    public static MemberInfo from(Member member) {
        return MemberInfo.builder()
                .id(member.getId())
                .email(member.getEmail())
                .role(member.getRole())
                .build();
    }
}
