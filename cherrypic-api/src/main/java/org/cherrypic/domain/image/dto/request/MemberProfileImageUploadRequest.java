package org.cherrypic.domain.image.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.cherrypic.domain.image.enums.ImageFileExtension;
import org.cherrypic.global.annotation.Enum;

public record MemberProfileImageUploadRequest(
        @Enum(message = "이미지 파일의 확장자는 비워둘 수 없으며, PNG, JPG, JPEG, WEBP, HEIC, HEIF만 지원됩니다.")
                @Schema(description = "이미지 파일의 확장자", defaultValue = "JPEG")
                ImageFileExtension imageFileExtension,
        @NotBlank(message = "MD5 해시값은 비워둘 수 없습니다,")
                @Schema(description = "S3 업로드시 사진의 변형을 확인하기 위한 md5 해시")
                String md5Hash) {}
