package org.cherrypic.domain.event.repository;

import org.cherrypic.domain.event.dto.EventListResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.springframework.data.domain.Slice;

public interface EventRepositoryCustom {
    Slice<EventListResponse> findAllByAlbumId(
            Long albumId, Long lastEventId, int size, SortDirection direction);
}
