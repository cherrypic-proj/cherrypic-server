package org.cherrypic.domain.event.repository;

import org.cherrypic.domain.event.dto.response.EventImageListResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.springframework.data.domain.Slice;

public interface EventImageRepositoryCustom {
    Slice<EventImageListResponse> findAllByEventId(
            Long eventId, Long lastEventImageId, int size, SortDirection direction);
}
