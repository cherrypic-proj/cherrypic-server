package org.cherrypic.domain.subscription.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record SubscriptionRenewRequest(
        @NotNull(message = "결제 ID는 비워둘 수 없습니다.")
                @Schema(description = "결제 검증 후 받은 결제 ID", example = "1")
                Long paymentId) {}
