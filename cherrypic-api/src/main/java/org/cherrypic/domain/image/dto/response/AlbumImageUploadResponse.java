package org.cherrypic.domain.image.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record AlbumImageUploadResponse(
        @Schema(description = "생성된 presigned url 리스트", example = "1") List<String> urls) {
    public static AlbumImageUploadResponse of(List<String> urls) {
        return new AlbumImageUploadResponse(urls);
    }
}
