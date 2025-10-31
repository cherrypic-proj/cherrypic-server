package org.cherrypic.domain.image.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record TempAlbumImagesConfirmResponse(
        @Schema(description = "생성된 임시 앨범 이미지들의 ID 리스트") List<Long> tempAlbumImageIds) {
    public static TempAlbumImagesConfirmResponse of(List<Long> tempAlbumImageIds) {
        return new TempAlbumImagesConfirmResponse(tempAlbumImageIds);
    }
}
