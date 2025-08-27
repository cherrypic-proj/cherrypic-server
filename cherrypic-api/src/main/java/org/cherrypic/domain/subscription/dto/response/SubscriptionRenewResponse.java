package org.cherrypic.domain.subscription.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import org.cherrypic.subscription.entity.Subscription;
import org.cherrypic.subscription.enums.SubscriptionStatus;

public record SubscriptionRenewResponse(
        @JsonFormat(
                        shape = JsonFormat.Shape.STRING,
                        pattern = "yyyy-MM-dd",
                        timezone = "Asia/Seoul")
                @Schema(description = "구독 시작일", example = "2024-08-21")
                LocalDateTime subscriptionStartAt,
        @JsonFormat(
                        shape = JsonFormat.Shape.STRING,
                        pattern = "yyyy-MM-dd",
                        timezone = "Asia/Seoul")
                @Schema(description = "구독 종료일", example = "2024-09-20")
                LocalDateTime subscriptionEndAt,
        @JsonFormat(
                        shape = JsonFormat.Shape.STRING,
                        pattern = "yyyy-MM-dd",
                        timezone = "Asia/Seoul")
                @Schema(description = "다음 결제일", example = "2024-09-21")
                LocalDateTime subscriptionNextBillingAt,
        @Schema(description = "구독 상태", example = "ACTIVE") SubscriptionStatus status) {
    public static SubscriptionRenewResponse from(Subscription subscription) {
        return new SubscriptionRenewResponse(
                subscription.getStartAt(),
                subscription.getEndAt(),
                subscription.getNextBillingAt(),
                subscription.getStatus());
    }
}
