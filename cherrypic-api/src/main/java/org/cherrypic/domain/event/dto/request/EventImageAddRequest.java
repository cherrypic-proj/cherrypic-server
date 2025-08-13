package org.cherrypic.domain.event.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record EventImageAddRequest(
        @NotEmpty(message = "추가할 이미지 ID는 비워둘 수 없습니다.")
                @Schema(description = "이벤트에 추가할 이미지들의 ID", example = "{1,2,3,4}")
                Set<Long> imageIds) {}
