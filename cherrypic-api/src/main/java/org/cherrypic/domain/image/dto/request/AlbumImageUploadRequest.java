package org.cherrypic.domain.image.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.cherrypic.domain.image.enums.FileExtension;
import org.cherrypic.global.annotation.Enum;

public record AlbumImageUploadRequest(
        @NotNull(message = "이미지 파일들의 용량은 비워둘 수 없습니다.")
                @Schema(description = "업로드 하는 이미지들의 용량 총합(GB)", example = "1.23")
                BigDecimal capacity,
        @NotEmpty(message = "앨범 이미지 업로드 요청은 비워둘 수 없습니다.") @Valid @Schema(description = "업로드 요청 리스트")
                List<AlbumImageUploadRequest.payload> payloads) {
    public record payload(
            @Enum(
                            message =
                                    "파일의 확장자는 비워둘 수 없으며, "
                                            + "이미지(PNG, JPG, JPEG, WEBP, HEIC, HEIF)와 동영상(MP4, WEBM, MOV, MKV, HEVC)만 지원됩니다.")
                    @Schema(description = "파일의 확장자", defaultValue = "JPEG")
                    FileExtension fileExtension,
            @NotBlank(message = "MD5 해시값은 비워둘 수 없습니다.")
                    @Schema(description = "S3 업로드시 사진의 변형을 확인하기 위한 md5 해시")
                    String md5Hashes,
            @Schema(description = "사진이 찍힌 시간, 정보가 없다면 null을 넣어주세요.") LocalDateTime generatedAt) {}
}
