package org.cherrypic.domain.image.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cherrypic.exception.BaseErrorCode;

@Getter
@AllArgsConstructor
public enum ImageErrorCode implements BaseErrorCode {
    SOME_IMAGES_ARE_NOT_FOUND(404, "존재하지 않는 이미지를 포함하고 있습니다."),
    SOME_IMAGES_HAS_EVENT(400, "이미 이벤트에 소속된 이미지를 포함하고 있습니다."),
    SOME_IMAGES_NOT_FROM_CURRENT_ALBUM(400, "앨범 소속이 아닌 이미지를 포함하고 있습니다."),
    SOME_IMAGES_HAS_CONFLICT(409, "다른 요청에서 조작된 이미지를 포함하고 있습니다.");

    private final int status;
    private final String message;
}
