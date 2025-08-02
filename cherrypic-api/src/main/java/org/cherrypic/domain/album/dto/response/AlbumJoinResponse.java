package org.cherrypic.domain.album.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.participant.enums.ParticipantRole;

public record AlbumJoinResponse(
        @Schema(description = "참가자 ID", example = "1") Long participantId,
        @Schema(description = "앨범 ID", example = "1") Long albumId,
        @Schema(description = "회원 ID", example = "1") Long memberId,
        @Schema(description = "참가자 권한", example = "STANDARD") ParticipantRole role) {
    public static AlbumJoinResponse from(Participant participant) {
        return new AlbumJoinResponse(
                participant.getId(),
                participant.getAlbum().getId(),
                participant.getMember().getId(),
                participant.getRole());
    }
}
