package org.cherrypic.domain.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record EventImageListResponse(
        @Schema(description = "이벤트 이미지 ID", example = "1") Long eventImageId,
        @Schema(description = "이미지 url", example = "https://example.jpg") String imageUrl) {}
