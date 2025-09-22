package org.cherrypic.domain.album.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import org.cherrypic.album.enums.AlbumType;
import org.cherrypic.subscription.enums.SubscriptionStatus;

public record AlbumListResponse(
        @Schema(description = "앨범 ID", example = "1") Long albumId,
        @Schema(description = "앨범 이름", example = "연인 앨범") String title,
        @Schema(description = "앨범 커버 URL", example = "https://example.jpg") String coverUrl,
        @Schema(description = "앨범 유형", example = "BASIC") AlbumType type,
        @Schema(description = "앨범 구독 가격", example = "5900") Integer price,
        @Schema(description = "구독 상태", example = "ACTIVE") SubscriptionStatus status,
        @JsonFormat(
                        shape = JsonFormat.Shape.STRING,
                        pattern = "yyyy-MM-dd",
                        timezone = "Asia/Seoul")
                @Schema(description = "앨범 생성일", example = "2025-08-01")
                LocalDateTime createdAt,
        @Schema(description = "즐겨찾기 상태", example = "true") Boolean marked) {
    public static AlbumListResponse of(
            Long albumId,
            String title,
            String coverUrl,
            AlbumType type,
            SubscriptionStatus status,
            LocalDateTime createdAt,
            Boolean marked) {
        return new AlbumListResponse(
                albumId, title, coverUrl, type, type.getPrice(), status, createdAt, marked);
    }
}
