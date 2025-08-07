package org.cherrypic.domain.event.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cherrypic.exception.BaseErrorCode;

@Getter
@AllArgsConstructor
public enum EventErrorCode implements BaseErrorCode {
    EVENT_NOT_FOUND(404, "존재하지 않는 이벤트입니다."),
    EVENT_NOT_FOUND_IN_ALBUM(404, "앨범에 해당 이벤트가 존재하지 않습니다.");

    private final int status;
    private final String message;
}
