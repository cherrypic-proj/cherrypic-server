package org.cherrypic.domain.image.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ImageUploadUrlResponse(@Schema(description = "Presigned URL") String presignedUrl) {
    public static ImageUploadUrlResponse of(String presignedUrl) {
        return new ImageUploadUrlResponse(presignedUrl);
    }
}
