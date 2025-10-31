package org.cherrypic.domain.image.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ImageConfirmRequest(
        @NotBlank(message = "이미지 url을 비워둘 수 없습니다.")
                @Schema(description = "검증하고자 하는 이미지 url", example = "https://example.jpg")
                String imageUrl) {}
