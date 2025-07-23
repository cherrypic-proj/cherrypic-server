package org.cherrypic.domain.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.cherrypic.album.enums.AlbumPlan;

public record PaymentReadyRequest(
        @NotNull(message = "앨범 구독 플랜은 비워둘 수 없으며, PRO, PREMIUM만 지원됩니다.")
                @Schema(description = "앨범 구독 플랜", defaultValue = "PRO")
                AlbumPlan plan) {}
