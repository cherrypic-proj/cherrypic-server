package org.cherrypic.domain.participant.exception;

import org.cherrypic.exception.BaseCustomException;

public class ParticipantException extends BaseCustomException {
    public ParticipantException(ParticipantErrorCode errorCode) {
        super(errorCode);
    }
}
