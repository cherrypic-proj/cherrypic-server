package org.cherrypic.domain.image.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ImageListResponse(
        @Schema(description = "이미지 ID", example = "1") Long imageId,
        @Schema(description = "이미지 url", example = "https://example.jpg") String imageUrl) {}
