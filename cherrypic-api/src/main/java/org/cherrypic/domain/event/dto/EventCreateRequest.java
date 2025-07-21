package org.cherrypic.domain.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;

public record EventCreateRequest(
        @NotBlank(message = "엘범 ID는 비워둘 수 없습니다.")
                @Schema(description = "이벤트가 속한 엘범의 ID", example = "1L")
                Long albumId,
        @NotBlank(message = "이벤트 이름은 비워둘 수 없습니다.")
                @Max(100)
                @Schema(description = "이벤트의 이름", example = "일본 여행")
                String title) {}
