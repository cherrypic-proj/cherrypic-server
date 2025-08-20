package org.cherrypic.domain.album.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import org.cherrypic.album.enums.AlbumPlan;

public record AlbumGetResponse(
        @Schema(description = "앨범 제목", example = "맛집 탐방") String title,
        @Schema(description = "앨범 커버 사진 url", example = "https://example.jpg") String coverUrl,
        @Schema(description = "앨범 플랜", example = "PRO") AlbumPlan albumPlan,
        @Schema(description = "사용한 용량 (GB)", example = "2")
                @JsonFormat(shape = JsonFormat.Shape.STRING)
                BigDecimal capacityUsed,
        @Schema(description = "총 용량 (GB)", example = "3") BigDecimal totalCapacity,
        @Schema(description = "앨범 Host의 이름", example = "김OO") String hostName,
        @Schema(description = "앨범의 참여자 수", example = "6") Integer numOfParticipants) {
    public static AlbumGetResponse of(
            String title,
            String coverUrl,
            AlbumPlan albumPlan,
            BigDecimal capacityUsed,
            BigDecimal totalCapacity,
            String hostName,
            Integer numOfParticipants) {
        return new AlbumGetResponse(
                title,
                coverUrl,
                albumPlan,
                capacityUsed,
                totalCapacity,
                hostName,
                numOfParticipants);
    }
}
