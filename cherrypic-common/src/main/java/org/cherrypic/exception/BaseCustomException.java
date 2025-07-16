package org.cherrypic.exception;

import lombok.Getter;

@Getter
public class BaseCustomException extends RuntimeException {

    private final BaseErrorCode errorCode;

    public BaseCustomException(BaseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
