package org.cherrypic.domain.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cherrypic.album.enums.AlbumPlan;

public record PaymentReadyResponse(
        @Schema(description = "앨범 구독 플랜", defaultValue = "PRO") AlbumPlan plan,
        @Schema(description = "구독 플랜 가격", example = "3900") int price,
        @Schema(description = "결제 요청 고유 ID", example = "album_20250723_pro_1_c4f1a2b3d5e6")
                String merchantUid,
        @Schema(description = "구매자 이름", example = "최현태") String buyerName) {
    public static PaymentReadyResponse of(
            AlbumPlan plan, int price, String merchantUid, String buyerName) {
        return new PaymentReadyResponse(plan, price, merchantUid, buyerName);
    }
}
