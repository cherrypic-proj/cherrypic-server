package org.cherrypic.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GlobalErrorCode implements ErrorCode {
    METHOD_NOT_ALLOWED(405, "지원하지 않는 HTTP method 입니다."),
    ;

    private final int status;
    private final String message;
}
