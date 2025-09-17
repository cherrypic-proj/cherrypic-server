package org.cherrypic.domain.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cherrypic.member.entity.Member;
import org.cherrypic.member.enums.MemberRole;
import org.cherrypic.member.enums.MemberStatus;

public record MemberInfoResponse(
        @Schema(description = "회원 아이디", example = "1") Long memberId,
        @Schema(description = "소셜 로그인 제공자", example = "https://kauth.kakao.com")
                String oauthProvider,
        @Schema(description = "회원 닉네임", example = "최현태") String nickname,
        @Schema(
                        description = "회원 프로필 사진",
                        example =
                                "http://k.kakaocdn.net/dn/ceTrU6/btsL0V0mhKO/DGqAZKAK/img_110x110.jpg")
                String profileImageUrl,
        @Schema(description = "회원 상태", example = "NORMAL") MemberStatus status,
        @Schema(description = "회원 역할", example = "ROLE_USER") MemberRole role,
        @Schema(description = "회원 로컬 사진 삭제 동의", example = "true") Boolean localImageDeletion) {
    public static MemberInfoResponse from(Member member) {
        return new MemberInfoResponse(
                member.getId(),
                member.getOauthInfo().getOauthProvider(),
                member.getNickname(),
                member.getProfileImageUrl(),
                member.getStatus(),
                member.getRole(),
                member.getLocalImageDeletion());
    }
}
