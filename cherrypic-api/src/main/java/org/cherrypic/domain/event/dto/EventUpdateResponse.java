package org.cherrypic.domain.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cherrypic.event.entity.Event;

public record EventUpdateResponse(
        @Schema(description = "이벤트 ID", example = "1") Long eventId,
        @Schema(description = "이벤트 제목", example = "일본 여행") String title,
        @Schema(description = "이벤트 커버 URL", example = "https://example.jpg") String coverUrl) {
    public static EventUpdateResponse from(Event event) {
        return new EventUpdateResponse(event.getId(), event.getTitle(), event.getCoverUrl());
    }
}
