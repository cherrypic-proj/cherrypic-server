package org.cherrypic.domain.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import org.cherrypic.payment.enums.PaymentPurpose;

public record PaymentUnlinkedResponse(
        @Schema(description = "결제 ID", example = "1") Long paymentId,
        @Schema(description = "결제 금액", example = "5900") Integer amount,
        @Schema(description = "결제 목적", example = "CREATION") PaymentPurpose purpose,
        @Schema(description = "결제 완료 시각", example = "2025-08-31T12:34:00") LocalDateTime paidAt) {}
