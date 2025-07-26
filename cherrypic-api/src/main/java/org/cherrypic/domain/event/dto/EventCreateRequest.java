package org.cherrypic.domain.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EventCreateRequest(
        @NotNull(message = "앨범 ID는 비워둘 수 없습니다.")
                @Schema(description = "이벤트가 속한 엘범의 ID", example = "1")
                Long albumId,
        @NotBlank(message = "이벤트 이름은 비워둘 수 없습니다.")
                @Size(max = 20, message = "이벤트 이름은 최대 20자까지 가능합니다.")
                @Schema(description = "이벤트의 이름", example = "일본 여행")
                String title,
        @Schema(description = "이벤트 커버 URL", example = "https://example.jpg") String coverUrl) {}
