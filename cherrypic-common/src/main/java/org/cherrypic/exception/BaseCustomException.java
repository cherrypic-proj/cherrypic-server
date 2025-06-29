package org.cherrypic.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BaseCustomException extends RuntimeException {

    private final BaseErrorCode errorCode;
}
