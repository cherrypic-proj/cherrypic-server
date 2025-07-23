package org.cherrypic.domain.payment.exception;

import org.cherrypic.exception.BaseCustomException;

public class PaymentException extends BaseCustomException {
    public PaymentException(PaymentErrorCode errorCode) {
        super(errorCode);
    }
}
