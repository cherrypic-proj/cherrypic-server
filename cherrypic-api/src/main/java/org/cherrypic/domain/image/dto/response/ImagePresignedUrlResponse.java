package org.cherrypic.domain.image.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ImagePresignedUrlResponse(
        @Schema(description = "Presigned URL") String presignedUrl) {
    public static ImagePresignedUrlResponse of(String presignedUrl) {
        return new ImagePresignedUrlResponse(presignedUrl);
    }
}
