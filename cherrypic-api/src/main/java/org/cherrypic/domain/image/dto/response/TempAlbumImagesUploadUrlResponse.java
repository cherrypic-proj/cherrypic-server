package org.cherrypic.domain.image.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record TempAlbumImagesUploadUrlResponse(
        @Schema(description = "생성된 presigned url 리스트", example = "[1,2,3,4]") List<String> urls) {
    public static TempAlbumImagesUploadUrlResponse of(List<String> urls) {
        return new TempAlbumImagesUploadUrlResponse(urls);
    }
}
