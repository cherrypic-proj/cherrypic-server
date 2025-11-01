package org.cherrypic.domain.image.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record AlbumImagesUploadCompleteResponse(
        @Schema(description = "로컬 사진 삭제 허용 여부") Boolean localImageDeletion,
        @Schema(description = "생성된 이미지들의 ID 리스트") List<Long> imageIds) {
    public static AlbumImagesUploadCompleteResponse of(
            List<Long> imageIds, Boolean localImageDeletion) {
        return new AlbumImagesUploadCompleteResponse(localImageDeletion, imageIds);
    }
}
