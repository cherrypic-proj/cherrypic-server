package org.cherrypic.domain.image.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import org.cherrypic.domain.image.enums.ImageFileExtension;

public record AlbumImageUploadRequest(
        @NotEmpty(message = "이미지 파일들의 확장자는 비워둘 수 없습니다.")
                @Schema(description = "이미지 파일들의 확장자", defaultValue = "[JPEG,JPG]")
                List<ImageFileExtension> imageFileExtensions,
        @NotNull(message = "이미지 파일들의 용량은 비워둘 수 없습니다.")
                @Schema(description = "업로드 하는 이미지들의 용량합(GB)", example = "1.23")
                BigDecimal capacity,
        @NotEmpty(message = "MD5 해시값은 비워둘 수 없습니다,")
                @Schema(description = "S3 업로드시 사진의 변형을 확인하기 위한 md5 해시 List")
                List<String> md5Hashes) {}
