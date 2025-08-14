package org.cherrypic.domain.event.service;

import org.cherrypic.domain.event.dto.request.EventCreateRequest;
import org.cherrypic.domain.event.dto.request.EventImageAddRequest;
import org.cherrypic.domain.event.dto.request.EventImageRemoveRequest;
import org.cherrypic.domain.event.dto.request.EventUpdateRequest;
import org.cherrypic.domain.event.dto.response.EventCreateResponse;
import org.cherrypic.domain.event.dto.response.EventListResponse;
import org.cherrypic.domain.event.dto.response.EventUpdateResponse;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;

public interface EventService {
    EventCreateResponse createEvent(EventCreateRequest request);

    EventUpdateResponse updateEvent(Long eventId, EventUpdateRequest request);

    SliceResponse<EventListResponse> getAlbumEvents(
            Long albumId, Long lastEventId, int size, SortDirection direction);

    void deleteEvent(Long eventId);

    void addImages(Long eventId, EventImageAddRequest request);

    void removeImages(Long eventId, EventImageRemoveRequest request);
}
