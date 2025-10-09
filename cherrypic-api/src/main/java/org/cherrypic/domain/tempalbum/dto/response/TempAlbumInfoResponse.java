package org.cherrypic.domain.tempalbum.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.cherrypic.global.util.StorageUnitConverter;

public record TempAlbumInfoResponse(
        @Schema(description = "앨범 제목", example = "맛집 탐방") String title,
        @Schema(description = "사용한 용량 (GB)", example = "2.4") BigDecimal capacityUsedGb,
        @Schema(description = "총 용량 (GB)", example = "3") BigDecimal totalCapacityGb,
        @Schema(description = "만료 일자", example = "2025-01-01") LocalDate expiredAt,
        @Schema(description = "QR용 링크", example = "https://example.com") String webUrl) {
    public static TempAlbumInfoResponse of(
            String title,
            BigDecimal capacityUsedMb,
            BigDecimal totalCapacityMb,
            LocalDate expiredAt,
            String webUrl) {
        return new TempAlbumInfoResponse(
                title,
                StorageUnitConverter.mbToGb(capacityUsedMb),
                StorageUnitConverter.mbToGb(totalCapacityMb),
                expiredAt,
                webUrl);
    }
}
