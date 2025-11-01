package org.cherrypic.domain.image.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record AlbumImagesPresignedUrlResponse(
        @Schema(description = "생성된 presigned url 리스트") List<String> urls) {
    public static AlbumImagesPresignedUrlResponse of(List<String> urls) {
        return new AlbumImagesPresignedUrlResponse(urls);
    }
}
