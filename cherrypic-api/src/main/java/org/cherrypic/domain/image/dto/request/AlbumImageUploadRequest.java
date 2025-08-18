package org.cherrypic.domain.image.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import org.cherrypic.domain.image.enums.ImageFileExtension;
import org.cherrypic.global.annotation.Enum;

public record AlbumImageUploadRequest(
        @NotEmpty(message = "이미지 파일들의 확장자는 비워둘 수 없습니다.")
                @Schema(description = "이미지 파일들의 확장자", defaultValue = "JPEG")
                List<
                                @Enum(
                                        message =
                                                "이미지 파일의 확장자는 PNG, JPG, JPEG, WEBP, HEIC, HEIF만 지원됩니다.")
                                ImageFileExtension>
                        imageFileExtensions,
        @Schema(description = "업로드 하는 이미지들의 용량합(GB)", example = "1.23") BigDecimal capacity,
        @NotNull(message = "앨범 ID는 비워둘 수 없습니다.")
                @Schema(description = "이미지를 업로드 하고자 하는 앨범 ID", example = "1")
                Long albumId) {}
