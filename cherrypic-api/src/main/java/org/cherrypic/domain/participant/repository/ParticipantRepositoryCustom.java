package org.cherrypic.domain.participant.repository;

import org.cherrypic.domain.participant.dto.response.ParticipantListResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.springframework.data.domain.Slice;

public interface ParticipantRepositoryCustom {
    Slice<ParticipantListResponse> findAllByAlbumId(
            Long albumId, Long lastParticipantId, int size, SortDirection direction);
}
