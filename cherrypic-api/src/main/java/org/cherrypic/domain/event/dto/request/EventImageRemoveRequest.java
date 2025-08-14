package org.cherrypic.domain.event.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record EventImageRemoveRequest(
        @NotEmpty(message = "제거할 이벤트 이미지 ID는 비워둘 수 없습니다.")
                @Schema(description = "이벤트에서 제거할 이벤트 이미지 ID", example = "[1,2,3,4]")
                List<Long> eventImageIds) {}
