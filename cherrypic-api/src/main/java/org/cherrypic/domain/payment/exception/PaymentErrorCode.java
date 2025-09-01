package org.cherrypic.domain.payment.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cherrypic.exception.BaseErrorCode;

@Getter
@AllArgsConstructor
public enum PaymentErrorCode implements BaseErrorCode {
    UNSUPPORTED_PAYMENT(400, "BASIC 유형은 결제를 지원하지 않습니다."),

    PAYMENT_NOT_FOUND(404, "결제 정보가 존재하지 않습니다."),

    AMOUNT_MISMATCH(400, "결제 금액이 일치하지 않아 검증에 실패했습니다."),
    NOT_PAID(400, "결제가 완료되지 않아 검증에 실패했습니다."),

    IAMPORT_API_UNAVAILABLE(503, "결제 대행 시스템(Iamport)과의 통신에 실패했습니다. 잠시 후 다시 시도해주세요."),

    PAYMENT_MEMBER_MISMATCH(403, "결제한 사용자와 일치하지 않습니다."),

    DOWNGRADE_NOT_ALLOWED(400, "현재 앨범 유형보다 낮은 유형으로 결제를 진행할 수 없습니다."),

    UNLINKED_PAYMENT_ALREADY_EXISTS(400, "아직 사용되지 않은 완료된 결제 내역이 존재합니다. 앨범 생성 또는 구독을 먼저 완료해주세요."),
    ;

    private final int status;
    private final String message;
}
