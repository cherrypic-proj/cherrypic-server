package org.cherrypic.domain.image.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cherrypic.exception.BaseErrorCode;

@Getter
@AllArgsConstructor
public enum ImageErrorCode implements BaseErrorCode {
    IMAGES_NOT_FOUND(404, "존재하지 않는 이미지를 포함하고 있습니다."),
    IMAGES_FROM_OTHER_ALBUM(400, "앨범 소속이 아닌 이미지를 포함하고 있습니다."),
    IMAGE_DELETED(409, "이미지 관련 작업중 이미지가 삭제되었습니다."),
    IMAGE_CONFLICT(409, "이미지 동시성 문제 발생");

    private final int status;
    private final String message;
}
