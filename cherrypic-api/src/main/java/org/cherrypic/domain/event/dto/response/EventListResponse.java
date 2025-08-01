package org.cherrypic.domain.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record EventListResponse(
        @Schema(description = "이벤트 ID", example = "1") Long eventId,
        @Schema(description = "이벤트 이름", example = "일본 여행") String title,
        @Schema(description = "이벤트 커버 URL", example = "https://example.jpg") String coverUrl,
        @Schema(description = "이벤트 사진 수", example = "4") int numberOfImages) {}
