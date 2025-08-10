package org.cherrypic.domain.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.event.dto.request.EventCreateRequest;
import org.cherrypic.domain.event.dto.request.EventIncludeRequest;
import org.cherrypic.domain.event.dto.request.EventUpdateRequest;
import org.cherrypic.domain.event.dto.response.EventCreateResponse;
import org.cherrypic.domain.event.dto.response.EventListResponse;
import org.cherrypic.domain.event.dto.response.EventUpdateResponse;
import org.cherrypic.domain.event.service.EventService;
import org.cherrypic.global.annotation.PageSize;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Tag(name = "4. 이벤트 API", description = "이벤트 관련 API입니다.")
@Validated
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

    @GetMapping
    @Operation(summary = "이벤트 목록 조회", description = "회원이 참여 중인 앨범의 이벤트를 커서 기반 페이징 방식으로 조회합니다.")
    public SliceResponse<EventListResponse> eventsGet(
            @Parameter(description = "조회중인 앨범의 ID") @RequestParam Long albumId,
            @Parameter(description = "이전 페이지의 마지막 이벤트 ID (첫 요청 시 생략)")
                    @RequestParam(required = false)
                    Long lastEventId,
            @Parameter(description = "페이지당 조회할 이벤트의 수") @RequestParam @PageSize Integer size,
            @Parameter(description = "정렬 방향 (ASC: 오래된순, DESC: 최신순)")
                    @RequestParam(defaultValue = "DESC")
                    SortDirection direction) {
        return eventService.getAlbumEvents(albumId, lastEventId, size, direction);
    }

    @PatchMapping("/include")
    @Operation(summary = "이벤트에 이미지 추가", description = "앨범의 이미지를 이벤트로 추가합니다.")
    public ResponseEntity<Void> eventInclude(@Valid @RequestBody EventIncludeRequest request) {
        eventService.includeEvent(request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{eventId}")
    @Operation(summary = "이벤트 삭제", description = "기존 이벤트를 삭제합니다.")
    public ResponseEntity<Void> eventDelete(@PathVariable Long eventId) {
        eventService.deleteEvent(eventId);
        return ResponseEntity.noContent().build();
    }
}
