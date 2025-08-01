package org.cherrypic.domain.album.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlbumUpdateRequest(
        @NotBlank(message = "앨범 이름은 비워둘 수 없습니다.")
                @Schema(description = "앨범 이름", example = "연인 앨범")
                @Size(max = 20, message = "앨범 이름은 최대 20자까지 가능합니다.")
                String title,
        @Schema(description = "앨범 커버 URL", example = "https://example.jpg") String coverUrl) {}
