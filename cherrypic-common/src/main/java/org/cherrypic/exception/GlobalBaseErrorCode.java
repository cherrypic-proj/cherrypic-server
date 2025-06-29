package org.cherrypic.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GlobalBaseErrorCode implements BaseErrorCode {
    METHOD_NOT_ALLOWED(405, "지원하지 않는 HTTP method 입니다."),
    METHOD_ARGUMENT_TYPE_MISMATCH(400, "요청한 값의 타입이 잘못되어 처리할 수 없습니다."),
    INTERNAL_SERVER_ERROR(500, "서버 오류, 관리자에게 문의하세요"),
    ;

    private final int status;
    private final String message;
}
