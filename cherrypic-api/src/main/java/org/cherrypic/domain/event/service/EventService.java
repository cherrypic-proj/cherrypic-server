package org.cherrypic.domain.event.service;

import org.cherrypic.domain.event.dto.EventCreateRequest;
import org.cherrypic.domain.event.dto.EventCreateResponse;

public interface EventService {

    EventCreateResponse createEvent(EventCreateRequest request);
}
