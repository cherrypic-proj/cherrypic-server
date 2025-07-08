package org.cherrypic.domain.auth.exception;

import org.cherrypic.exception.BaseCustomException;

public class AuthException extends BaseCustomException {
    public AuthException(AuthErrorCode errorCode) {
        super(errorCode);
    }
}
