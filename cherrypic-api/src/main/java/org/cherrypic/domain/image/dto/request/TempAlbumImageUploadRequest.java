package org.cherrypic.domain.image.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import org.cherrypic.global.annotation.Enum;
import org.cherrypic.s3.enums.FileExtension;

public record TempAlbumImageUploadRequest(
        @NotEmpty(message = "업로드할 피일들의 정보는 비워둘 수 없습니다.") @Valid @Schema(description = "업로드 요청 리스트")
                List<Payload> payloads) {
    public record Payload(
            @Enum(
                            message =
                                    "파일의 확장자는 비워둘 수 없으며, "
                                            + "이미지(PNG, JPG, JPEG, WEBP, HEIC, HEIF)와 동영상(MP4, WEBM, MOV, MKV, HEVC)만 지원됩니다.")
                    @Schema(description = "파일의 확장자", defaultValue = "JPEG")
                    FileExtension fileExtension,
            @NotBlank(message = "MD5 해시값은 비워둘 수 없습니다.")
                    @Schema(description = "S3 업로드시 파일의 변형을 확인하기 위한 md5 해시")
                    String md5Hashes,
            @NotNull(message = "파일의 용량은 비워둘 수 없습니다.")
                    @Schema(description = "업로드 하는 파일의 용량(GB)", example = "0.04")
                    BigDecimal capacity) {}
}
