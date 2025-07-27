package org.cherrypic.domain.payment.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cherrypic.exception.BaseErrorCode;

@Getter
@AllArgsConstructor
public enum PaymentErrorCode implements BaseErrorCode {
    UNSUPPORTED_PAYMENT_PLAN(400, "해당 플랜은 유료 결제가 필요하지 않습니다. PRO 또는 PREMIUM 플랜만 결제가 가능합니다."),

    PAYMENT_NOT_FOUND(404, "결제 정보가 존재하지 않습니다."),

    AMOUNT_MISMATCH(400, "결제 금액이 일치하지 않아 검증에 실패했습니다."),
    NOT_PAID(400, "결제가 완료되지 않아 검증에 실패했습니다."),

    IAMPORT_API_UNAVAILABLE(503, "결제 대행 시스템(Iamport)과의 통신에 실패했습니다. 잠시 후 다시 시도해주세요.");

    private final int status;
    private final String message;
}
