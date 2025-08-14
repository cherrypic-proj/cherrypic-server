package org.cherrypic.domain.event.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cherrypic.exception.BaseErrorCode;

@Getter
@AllArgsConstructor
public enum EventErrorCode implements BaseErrorCode {
    EVENT_NOT_FOUND(404, "존재하지 않는 이벤트입니다."),
    EVENT_DELETED(409, "이벤트 관련 작업 중 이벤트가 삭제되었습니다."),

    EVENT_IMAGES_NOT_IN_EVENT(400, "이벤트에 속해 있지 않은 이벤트 이미지가 포함되어 있습니다.");

    private final int status;
    private final String message;
}
