package org.cherrypic.domain.auth.dto;

import org.cherrypic.member.enums.MemberRole;

public record AccessTokenDto(Long memberId, MemberRole role, String tokenValue) {
    public static AccessTokenDto of(Long memberId, MemberRole role, String tokenValue) {
        return new AccessTokenDto(memberId, role, tokenValue);
    }
}
