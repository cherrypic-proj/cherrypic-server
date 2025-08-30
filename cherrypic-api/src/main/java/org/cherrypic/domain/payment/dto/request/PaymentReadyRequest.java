package org.cherrypic.domain.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cherrypic.album.enums.AlbumType;
import org.cherrypic.global.annotation.Enum;

public record PaymentReadyRequest(
        @Enum(message = "유료 앨범 유형은 비워둘 수 없으며, PRO, PREMIUM만 지원됩니다.")
                @Schema(description = "유료 앨범 유형", defaultValue = "PRO")
                AlbumType type,
        @Schema(description = "앨범 ID (갱신/업그레이드 시에만 사용)", example = "1") Long albumId) {}
