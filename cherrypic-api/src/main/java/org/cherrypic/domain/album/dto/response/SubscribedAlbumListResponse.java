package org.cherrypic.domain.album.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import org.cherrypic.album.enums.AlbumPlan;

public record SubscribedAlbumListResponse(
        @Schema(description = "앨범 ID", example = "1") Long albumId,
        @Schema(description = "앨범 이름", example = "연인 앨범") String title,
        @Schema(description = "앨범 플랜", example = "PRO") AlbumPlan plan,
        @Schema(description = "앨범 플랜 가격", example = "3900") Integer price,
        @JsonFormat(
                        shape = JsonFormat.Shape.STRING,
                        pattern = "yyyy-MM-dd",
                        timezone = "Asia/Seoul")
                @Schema(description = "앨범 생성일", example = "2024-08-21")
                LocalDateTime albumCreatedAt,
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
                LocalDateTime subscriptionNextBillingAt) {
    public static SubscribedAlbumListResponse of(
            Long albumId,
            String title,
            AlbumPlan plan,
            LocalDateTime albumCreatedAt,
            LocalDateTime subscriptionStartAt,
            LocalDateTime subscriptionEndAt,
            LocalDateTime subscriptionNextBillingAt) {
        return new SubscribedAlbumListResponse(
                albumId,
                title,
                plan,
                plan.getPrice(),
                albumCreatedAt,
                subscriptionStartAt,
                subscriptionEndAt,
                subscriptionNextBillingAt);
    }
}
