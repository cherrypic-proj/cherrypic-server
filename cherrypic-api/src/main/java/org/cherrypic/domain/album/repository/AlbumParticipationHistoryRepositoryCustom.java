package org.cherrypic.domain.album.repository;

import org.cherrypic.domain.member.dto.response.ParticipationHistoryResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.springframework.data.domain.Slice;

public interface AlbumParticipationHistoryRepositoryCustom {
    Slice<ParticipationHistoryResponse> findParticipationHistory(
            Long memberId, Long lastHistoryId, int size, SortDirection direction);
}
