package org.cherrypic.domain.image.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record UploadFailedFileDeleteRequest(
        @NotEmpty(message = "Presigned URL은 비워둘 수 없습니다.")
                @Schema(description = "업로드 실패한 Presigned URL들")
                List<String> presignedUrls) {}
