package org.cherrypic.domain.image.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record AlbumImageListResponse(
        @Schema(description = "이미지 ID", example = "1") Long imageId,
        @Schema(description = "이미지 url", example = "https://example.jpg") String imageUrl,
        @JsonFormat(
                        shape = JsonFormat.Shape.STRING,
                        pattern = "yyyy-MM-dd",
                        timezone = "Asia/Seoul")
                @Schema(description = "필터링 기준 날짜", example = "2025-10-01")
                LocalDateTime date) {}
