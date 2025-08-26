package org.cherrypic.domain.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cherrypic.album.enums.AlbumPlan;
import org.cherrypic.global.annotation.Enum;

public record PaymentReadyRequest(
        @Enum(message = "앨범 구독 플랜은 비워둘 수 없으며, PRO, PREMIUM만 지원됩니다.")
                @Schema(description = "앨범 구독 플랜", defaultValue = "PRO")
                AlbumPlan plan,
        @Schema(description = "앨범 ID (갱신/업그레이드 시에만 사용)", example = "1") Long albumId) {}
