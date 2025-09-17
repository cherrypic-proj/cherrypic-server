package org.cherrypic.domain.payment.dto.event;

import java.time.LocalDateTime;

public record PaymentVerifyEvent(Long paymentId, LocalDateTime paidAt) {
    public static PaymentVerifyEvent of(Long paymentId, LocalDateTime paidAt) {
        return new PaymentVerifyEvent(paymentId, paidAt);
    }
}
