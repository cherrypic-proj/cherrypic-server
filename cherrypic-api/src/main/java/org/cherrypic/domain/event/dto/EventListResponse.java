package org.cherrypic.domain.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cherrypic.event.entity.Event;

public record EventListResponse(
        @Schema(description = "이벤트 ID", example = "1") Long eventId,
        @Schema(description = "이벤트 이름", example = "일본 여행") String title,
        @Schema(description = "이벤트 커버 URL", example = "https://example.jpg") String coverUrl,
        @Schema(description = "이벤트 사진 수", example = "4") int numberOfImage) {
    public static EventListResponse of(Event event, int numberOfImage) {
        return new EventListResponse(
                event.getId(), event.getTitle(), event.getCoverUrl(), numberOfImage);
    }
}
