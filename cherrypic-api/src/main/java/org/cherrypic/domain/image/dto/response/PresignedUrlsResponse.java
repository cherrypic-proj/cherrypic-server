package org.cherrypic.domain.image.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record PresignedUrlsResponse(
        @Schema(description = "Presigned URLs") List<String> presignedUrls) {
    public static PresignedUrlsResponse of(List<String> presignedUrls) {
        return new PresignedUrlsResponse(presignedUrls);
    }
}
