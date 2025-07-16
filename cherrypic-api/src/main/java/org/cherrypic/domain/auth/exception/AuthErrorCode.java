package org.cherrypic.domain.auth.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cherrypic.exception.BaseErrorCode;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {
    ID_TOKEN_VERIFICATION_FAILED(401, "ID 토큰 검증에 실패했습니다."),

    AUTH_NOT_EXIST(401, "인증 정보가 존재하지 않습니다."),
    AUTH_NOT_PARSABLE(500, "인증 정보 파싱에 실패했습니다."),

    REFRESH_TOKEN_NOT_FOUND(401, "리프레시 토큰이 존재하지 않습니다."),
    INVALID_REFRESH_TOKEN(401, "유효하지 않은 리프레시 토큰입니다. 다시 로그인해주세요."),
    ;

    private final int status;
    private final String message;
}
