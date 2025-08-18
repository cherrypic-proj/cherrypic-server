package org.cherrypic.domain.participant.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.participant.enums.ParticipantRole;

public record ParticipantListResponse(
        @Schema(description = "참가자 ID", example = "1") Long participantId,
        @Schema(description = "회원 닉네임", example = "최현태") String nickname,
        @Schema(
                        description = "회원 프로필 사진",
                        example =
                                "http://k.kakaocdn.net/dn/ceTrU6/btsL0V0mhKO/DGqAZKAK/img_110x110.jpg")
                String profileImageUrl,
        @Schema(description = "참가자 역할", example = "STANDARD") ParticipantRole role) {
    public static ParticipantListResponse from(Participant participant) {
        return new ParticipantListResponse(
                participant.getId(),
                participant.getMember().getNickname(),
                participant.getMember().getProfileImageUrl(),
                participant.getRole());
    }
}
