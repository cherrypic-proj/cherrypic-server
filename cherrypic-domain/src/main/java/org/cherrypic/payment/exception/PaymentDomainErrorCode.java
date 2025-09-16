package org.cherrypic.payment.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cherrypic.exception.BaseErrorCode;

@Getter
@AllArgsConstructor
public enum PaymentDomainErrorCode implements BaseErrorCode {
    NOT_PAID(400, "아직 결제가 완료되지 않았습니다."),
    ALREADY_USED_PAYMENT(400, "해당 결제는 이미 사용되었습니다."),
    ALREADY_CANCELED(400, "해당 결제는 이미 취소되었습니다."),
    ONLY_PAID_PAYMENT_CANCELABLE(400, "결제 취소는 완료된 결제만 가능합니다."),
    PAYMENT_PURPOSE_MISMATCH(400, "결제 목적이 요청하려는 작업과 일치하지 않습니다."),
    ;

    private final int status;
    private final String message;
}
