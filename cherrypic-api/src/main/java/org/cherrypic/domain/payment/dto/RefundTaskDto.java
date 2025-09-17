package org.cherrypic.domain.payment.dto;

import java.time.LocalDateTime;
import org.cherrypic.payment.entity.RefundTask;

public record RefundTaskDto(Long paymentId, LocalDateTime scheduledAt) {
    public static RefundTaskDto from(RefundTask refundTask) {
        return new RefundTaskDto(refundTask.getPaymentId(), refundTask.getScheduledAt());
    }
}
