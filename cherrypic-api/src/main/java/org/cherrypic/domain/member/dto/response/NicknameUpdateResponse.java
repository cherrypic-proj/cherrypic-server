package org.cherrypic.domain.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cherrypic.member.entity.Member;

public record NicknameUpdateResponse(
        @Schema(description = "닉네임", example = "최현태") String nickname) {
    public static NicknameUpdateResponse from(Member member) {
        return new NicknameUpdateResponse(member.getNickname());
    }
}
