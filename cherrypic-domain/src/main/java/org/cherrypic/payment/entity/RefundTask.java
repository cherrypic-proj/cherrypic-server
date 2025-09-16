package org.cherrypic.payment.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cherrypic.common.model.BaseTimeEntity;
import org.cherrypic.payment.enums.RefundTaskStatus;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefundTask extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull private Long paymentId;

    @NotNull
    @Enumerated(EnumType.STRING)
    private RefundTaskStatus status;

    @NotNull private LocalDateTime scheduledAt;

    private LocalDateTime executedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private RefundTask(Long paymentId, LocalDateTime scheduledAt, RefundTaskStatus status) {
        this.paymentId = paymentId;
        this.scheduledAt = scheduledAt;
        this.status = status;
    }

    public static RefundTask createRefundTask(Long paymentId, LocalDateTime scheduledAt) {
        return RefundTask.builder()
                .paymentId(paymentId)
                .scheduledAt(scheduledAt)
                .status(RefundTaskStatus.PENDING)
                .build();
    }
}
