package org.cherrypic.domain.image.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cherrypic.domain.image.enums.ImageFileExtension;
import org.cherrypic.global.annotation.Enum;

public record MemberProfileImageUploadRequest(
        @Enum(message = "이미지 파일의 확장자는 비워둘 수 없으며, PNG, JPG, JPEG만 지원됩니다.")
                @Schema(description = "이미지 파일의 확장자", defaultValue = "JPEG")
                ImageFileExtension imageFileExtension) {}
