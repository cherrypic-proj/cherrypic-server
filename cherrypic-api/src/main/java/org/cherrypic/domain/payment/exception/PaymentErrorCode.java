package org.cherrypic.domain.payment.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cherrypic.exception.BaseErrorCode;

@Getter
@AllArgsConstructor
public enum PaymentErrorCode implements BaseErrorCode {
    UNSUPPORTED_PAYMENT_PLAN(400, "해당 플랜은 유료 결제가 필요하지 않습니다. PRO 또는 PREMIUM 플랜만 결제가 가능합니다."),
    ;

    private final int status;
    private final String message;
}
