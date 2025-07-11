package org.cherrypic.domain.auth.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cherrypic.exception.BaseErrorCode;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {
    ID_TOKEN_VERIFICATION_FAILED(401, "ID 토큰 검증에 실패했습니다."),
    ;

    private final int status;
    private final String message;
}
