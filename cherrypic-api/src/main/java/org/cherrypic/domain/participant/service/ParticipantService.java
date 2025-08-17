package org.cherrypic.domain.participant.service;

import org.cherrypic.domain.participant.dto.response.ParticipantListResponse;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;

public interface ParticipantService {
    void leaveAlbum(Long albumId);

    void kickParticipant(Long albumId, Long participantId);

    SliceResponse<ParticipantListResponse> getParticipants(
            Long albumId, Long lastParticipantId, int size, SortDirection direction);
}
