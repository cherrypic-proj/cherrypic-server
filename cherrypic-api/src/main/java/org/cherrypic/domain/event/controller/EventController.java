package org.cherrypic.domain.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.event.dto.EventCreateRequest;
import org.cherrypic.domain.event.dto.EventCreateResponse;
import org.cherrypic.domain.event.dto.EventUpdateRequest;
import org.cherrypic.domain.event.dto.EventUpdateResponse;
import org.cherrypic.domain.event.service.EventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Tag(name = "4. 이벤트 API", description = "이벤트 관련 API입니다.")
public class EventController {

    private final EventService eventService;

    @PostMapping
    @Operation(summary = "이벤트 생성", description = "새로운 이벤트를 생성합니다.")
    public ResponseEntity<EventCreateResponse> eventCreate(
            @Valid @RequestBody EventCreateRequest request) {
        EventCreateResponse response = eventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{eventId}")
    @Operation(summary = "이벤트 수정", description = "기존 이벤트를 수정합니다.")
    public EventUpdateResponse eventUpdate(
            @PathVariable Long eventId, @Valid @RequestBody EventUpdateRequest request) {
        return eventService.updateEvent(eventId, request);
    }

    @DeleteMapping("/{eventId}")
    @Operation(summary = "이벤트 삭제", description = "기존 이벤트를 삭제합니다.")
    public ResponseEntity<Void> eventDelete(@PathVariable Long eventId) {
        eventService.deleteEvent(eventId);
        return ResponseEntity.noContent().build();
    }
}
