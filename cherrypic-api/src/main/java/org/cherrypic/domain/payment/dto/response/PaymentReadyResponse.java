package org.cherrypic.domain.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import org.cherrypic.album.enums.AlbumType;
import org.cherrypic.payment.enums.PaymentPurpose;

public record PaymentReadyResponse(
        @Schema(description = "유료 앨범 유형", defaultValue = "PRO") AlbumType type,
        @Schema(description = "앨범 구독 가격", example = "3900") Integer price,
        @Schema(description = "결제 요청 고유 ID", example = "album_20250723_pro_1_c4f1a2b3d5e6")
                String merchantUid,
        @Schema(description = "구매자 이름", example = "최현태") String buyerName,
        @Schema(description = "결제 목적", example = "RENEWAL") PaymentPurpose purpose,
        @JsonFormat(
                        shape = JsonFormat.Shape.STRING,
                        pattern = "yyyy-MM-dd",
                        timezone = "Asia/Seoul")
                @Schema(description = "다음 결제일", example = "2024-09-21")
                LocalDateTime nextBillingAt) {
    public static PaymentReadyResponse of(
            AlbumType type,
            Integer price,
            String merchantUid,
            String buyerName,
            PaymentPurpose purpose,
            LocalDateTime nextBillingAt) {
        return new PaymentReadyResponse(
                type, price, merchantUid, buyerName, purpose, nextBillingAt);
    }
}
