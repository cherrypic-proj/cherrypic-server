package org.cherrypic.domain.image.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record UploadImageListResponse(
        @Schema(description = "업로드된 이미지들의 정보 리스트") List<Payload> payloads) {
    public static UploadImageListResponse of(List<Payload> payloads) {
        return new UploadImageListResponse(payloads);
    }

    public record Payload(
            @Schema(description = "생성된 이미지의 ID") Long imageId,
            @Schema(description = "생성된 Presigned Url") String presignedUrl) {
        public static Payload of(Long imageId, String presignedUrl) {
            return new Payload(imageId, presignedUrl);
        }
    }
}
