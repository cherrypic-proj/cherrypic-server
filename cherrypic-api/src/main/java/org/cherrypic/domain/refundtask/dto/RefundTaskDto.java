package org.cherrypic.domain.refundtask.dto;

import java.time.LocalDateTime;
import org.cherrypic.refundtask.entity.RefundTask;

public record RefundTaskDto(Long paymentId, LocalDateTime scheduledAt) {
    public static RefundTaskDto from(RefundTask refundTask) {
        return new RefundTaskDto(refundTask.getPaymentId(), refundTask.getScheduledAt());
    }
}
