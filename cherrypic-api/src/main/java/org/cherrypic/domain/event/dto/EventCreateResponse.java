package org.cherrypic.domain.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cherrypic.event.entity.Event;

public record EventCreateResponse(
        @Schema(description = "이벤트 ID", example = "1L") Long eventId,
        @Schema(description = "이벤트 제목", example = "일본 여행") String eventTitle,
        @Schema(description = "이벤트 커버 URL", example = "https://example.jpg") String coverUrl) {
    public static EventCreateResponse from(Event event) {
        return new EventCreateResponse(event.getId(), event.getTitle(), event.getCoverUrl());
    }
}
