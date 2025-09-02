package org.cherrypic.domain.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record PaymentListResponse(
        @Schema(description = "결제 ID", example = "1") Long paymentId,
        @JsonFormat(
                        shape = JsonFormat.Shape.STRING,
                        pattern = "yyyy-MM-dd",
                        timezone = "Asia/Seoul")
                @Schema(description = "결제 완료일", example = "2024-08-21")
                LocalDate paidAt,
        @Schema(description = "결제 금액", example = "5900") Integer amount) {}
