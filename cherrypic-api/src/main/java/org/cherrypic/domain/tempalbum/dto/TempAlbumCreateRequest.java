package org.cherrypic.domain.tempalbum.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record TempAlbumCreateRequest(
        @NotEmpty(message = "임시 앨범으로 공유할 이미지 ID들은 비워둘 수 없습니다.")
                @Schema(description = "임시 앨범으로 공유할 이미지들의 ID", example = "[1,2,3,4]")
                List<Long> imageIds) {}
