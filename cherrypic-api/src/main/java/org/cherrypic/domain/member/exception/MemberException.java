package org.cherrypic.domain.member.exception;

import org.cherrypic.exception.BaseCustomException;

public class MemberException extends BaseCustomException {
    public MemberException(MemberErrorCode errorCode) {
        super(errorCode);
    }
}
