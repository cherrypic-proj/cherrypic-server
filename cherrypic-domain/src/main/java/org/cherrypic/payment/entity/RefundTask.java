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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", unique = true)
    private Payment payment;

    @NotNull
    @Enumerated(EnumType.STRING)
    private RefundTaskStatus status;

    @NotNull private LocalDateTime scheduledAt;

    private LocalDateTime executedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private RefundTask(Payment payment, LocalDateTime scheduledAt, RefundTaskStatus status) {
        this.payment = payment;
        this.scheduledAt = scheduledAt;
        this.status = status;
    }

    public static RefundTask createRefundTask(Payment payment, LocalDateTime scheduledAt) {
        return RefundTask.builder()
                .payment(payment)
                .scheduledAt(scheduledAt)
                .status(RefundTaskStatus.PENDING)
                .build();
    }

    public void complete(LocalDateTime executedAt) {
        this.status = RefundTaskStatus.COMPLETED;
        this.executedAt = executedAt;
    }
}
