package org.cherrypic.domain.image.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cherrypic.exception.BaseErrorCode;

@Getter
@AllArgsConstructor
public enum ImageErrorCode implements BaseErrorCode {
    IMAGES_NOT_FOUND(404, "존재하지 않는 이미지를 포함하고 있습니다."),
    IMAGES_IN_OTHER_ALBUM(400, "앨범 소속이 아닌 이미지를 포함하고 있습니다."),
    IMAGE_DELETED(409, "이미지 관련 작업 중 이미지가 삭제되었습니다."),
    IMAGE_CONFLICT(409, "예상치 못한 이미지 무결성 오류"),

    IMAGE_EXTENSION_AUTHORITY(403, "%s 앨범은 %s 확장자만 가능합니다."),
    IMAGE_CAPACITY_EXCEEDED(400, "앨범의 이미지 용량 허용치를 초과했습니다.");

    private final int status;
    private final String message;
}
