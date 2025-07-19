package org.cherrypic.domain.album.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.cherrypic.album.enums.AlbumType;

public record AlbumCreateRequest(
        @NotNull(message = "앨범 이름은 비워둘 수 없습니다.") @Schema(description = "앨범 이름", example = "연인 앨범")
                String title,
        @Schema(description = "앨범 커버 URL", example = "https://example.jpg") String coverUrl,
        @Schema(description = "앨범 유형", example = "SHARED") AlbumType type) {}
