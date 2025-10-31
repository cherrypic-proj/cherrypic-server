package org.cherrypic.domain.image.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record TempAlbumImageUploadResponse(
        @Schema(description = "생성된 presigned url 리스트", example = "[1,2,3,4]") List<String> urls) {
    public static TempAlbumImageUploadResponse of(List<String> urls) {
        return new TempAlbumImageUploadResponse(urls);
    }
}
