package org.cherrypic.domain.image.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AlbumImageDeleteRequest(
        @NotEmpty(message = "삭제하고자 하는 이미지 ID는 비워둘 수 없습니다.")
                @Schema(description = "삭제하고자 하는 이미지들의 ID", example = "[1,2,3,4]")
                List<Long> imageIds) {}
