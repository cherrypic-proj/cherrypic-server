package org.cherrypic.domain.image.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record TempAlbumImageListResponse(
        @Schema(description = "임시 앨범 이미지 ID", example = "1") Long tempAlbumImageId,
        @Schema(description = "임시 앨범 이미지 url", example = "https://example.jpg")
                String tempAlbumImageUrl,
        @JsonFormat(
                        shape = JsonFormat.Shape.STRING,
                        pattern = "yyyy-MM-dd",
                        timezone = "Asia/Seoul")
                @Schema(description = "필터링 기준 날짜", example = "2025-10-01")
                LocalDateTime date) {}
