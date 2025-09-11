package org.cherrypic.domain.image.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record TempAlbumImageDeleteRequest(
        @NotEmpty(message = "삭제하고자 하는 임시 앨범 이미지 ID들은 비워둘 수 없습니다.")
                @Schema(description = "삭제하고자 하는 임시 앨범 이미지들의 ID", example = "[1,2,3,4]")
                List<Long> tempAlbumImageIds) {}
