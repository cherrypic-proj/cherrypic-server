package org.cherrypic.domain.event.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cherrypic.exception.BaseErrorCode;

@Getter
@AllArgsConstructor
public enum EventErrorCode implements BaseErrorCode {
    NOT_ALBUM_PARTICIPANT(400, "사용자가 소속되지 않은 엘범에서 이벤트를 만들 수 없습니다.");

    private final int status;
    private final String message;
}
