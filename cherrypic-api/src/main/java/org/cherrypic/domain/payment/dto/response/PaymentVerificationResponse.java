package org.cherrypic.domain.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cherrypic.payment.entity.Payment;

public record PaymentVerificationResponse(
        @Schema(description = "결제 ID", example = "1") Long paymentId) {
    public static PaymentVerificationResponse from(Payment payment) {
        return new PaymentVerificationResponse(payment.getId());
    }
}
