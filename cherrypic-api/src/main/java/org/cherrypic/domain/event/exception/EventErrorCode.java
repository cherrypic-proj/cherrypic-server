package org.cherrypic.domain.event.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cherrypic.exception.BaseErrorCode;

@Getter
@AllArgsConstructor
public enum EventErrorCode implements BaseErrorCode {
    EVENT_NOT_FOUND(404, "존재하지 않는 이밴트입니다.");

    private final int status;
    private final String message;
}
