package org.cherrypic.domain.payment.dto.event;

import java.time.LocalDateTime;

public record RefundTaskScheduleEvent(Long paymentId, LocalDateTime paidAt) {
    public static RefundTaskScheduleEvent of(Long paymentId, LocalDateTime paidAt) {
        return new RefundTaskScheduleEvent(paymentId, paidAt);
    }
}
