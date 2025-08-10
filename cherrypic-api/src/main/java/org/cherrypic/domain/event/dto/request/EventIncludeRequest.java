package org.cherrypic.domain.event.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record EventIncludeRequest(
        @NotNull(message = "이벤트 ID는 비워둘 수 없습니다.")
                @Schema(description = "이미지를 추가할 이벤트 ID", example = "1")
                Long eventId,
        @NotEmpty(message = "추가할 이미지 ID는 비워둘 수 없습니다.")
                @Schema(description = "이벤트에 추가할 이미지들의 ID", example = "(1,2,3,4)")
                List<Long> imageIds) {}
