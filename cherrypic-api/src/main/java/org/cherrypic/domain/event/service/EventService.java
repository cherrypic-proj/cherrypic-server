package org.cherrypic.domain.event.service;

import org.cherrypic.domain.event.dto.EventCreateRequest;
import org.cherrypic.domain.event.dto.EventCreateResponse;
import org.cherrypic.domain.event.dto.EventUpdateRequest;
import org.cherrypic.domain.event.dto.EventUpdateResponse;

public interface EventService {
    EventCreateResponse createEvent(EventCreateRequest request);

    EventUpdateResponse updateEvent(Long eventId, EventUpdateRequest request);

    void deleteEvent(Long eventId);
}
