package org.cherrypic.domain.event.exception;

import org.cherrypic.exception.BaseCustomException;

public class EventException extends BaseCustomException {
    public EventException(EventErrorCode errorCode) {
        super(errorCode);
    }
}
