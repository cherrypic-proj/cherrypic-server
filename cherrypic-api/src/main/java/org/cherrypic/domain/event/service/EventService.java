package org.cherrypic.domain.event.service;

import org.cherrypic.domain.event.dto.*;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;

public interface EventService {
    EventCreateResponse createEvent(EventCreateRequest request);

    EventUpdateResponse updateEvent(Long eventId, EventUpdateRequest request);

    SliceResponse<EventListResponse> getAlbumEvents(
            Long albumId, Long lastEventId, int size, SortDirection direction);

    void deleteEvent(Long eventId);
}
