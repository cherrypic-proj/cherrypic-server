package org.cherrypic.domain.image.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record TempAlbumImageUploadListResponse(
        @Schema(description = "업로드된 임시 앨범 이미지들의 정보 리스트") List<Payload> payloads) {
    public static TempAlbumImageUploadListResponse of(List<Payload> payloads) {
        return new TempAlbumImageUploadListResponse(payloads);
    }

    public record Payload(
            @Schema(description = "생성된 임시 앨범 이미지의 ID") Long tempAlbumImageId,
            @Schema(description = "생성된 Presigned Url") String presignedUrl) {
        public static Payload of(Long imageId, String presignedUrl) {
            return new Payload(imageId, presignedUrl);
        }
    }
}
