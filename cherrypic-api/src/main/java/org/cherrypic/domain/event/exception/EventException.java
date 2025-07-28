package org.cherrypic.domain.event.exception;

import org.cherrypic.exception.BaseCustomException;
import org.cherrypic.exception.BaseErrorCode;

public class EventException extends BaseCustomException {
    public EventException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
