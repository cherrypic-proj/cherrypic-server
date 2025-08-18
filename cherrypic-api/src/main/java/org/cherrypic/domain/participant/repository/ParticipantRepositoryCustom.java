package org.cherrypic.domain.participant.repository;

import org.cherrypic.domain.participant.dto.response.ParticipantListResponse;
import org.springframework.data.domain.Slice;

public interface ParticipantRepositoryCustom {
    Slice<ParticipantListResponse> findParticipantsByAlbumIdExcludingMemberId(
            Long albumId,
            Long excludeMemberId,
            String lastNickname,
            Long lastParticipantId,
            int size);
}
