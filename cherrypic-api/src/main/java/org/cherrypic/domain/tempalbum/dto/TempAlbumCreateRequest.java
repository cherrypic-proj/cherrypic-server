package org.cherrypic.domain.tempalbum.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TempAlbumCreateRequest(
        @NotBlank(message = "임시 앨범 이름은 비워둘 수 없습니다.")
                @Schema(description = "임시 앨범 이름", example = "회사 공유용")
                @Size(max = 20, message = "임시 앨범 이름은 최대 20자까지 가능합니다.")
                String title) {}
