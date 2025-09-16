package org.cherrypic.payment.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cherrypic.payment.enums.RefundTaskStatus;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefundTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull private Long paymentId;

    @NotNull
    @Enumerated(EnumType.STRING)
    private RefundTaskStatus status;

    @NotNull private LocalDateTime scheduledAt;

    private LocalDateTime executedAt;
}
