package org.cherrypic.domain.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cherrypic.member.entity.Member;

public record MemberProfileUpdateResponse(
        @Schema(description = "닉네임", example = "최현태") String nickname,
        @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
                String profileImageUrl) {
    public static MemberProfileUpdateResponse from(Member member) {
        return new MemberProfileUpdateResponse(member.getNickname(), member.getProfileImageUrl());
    }
}
