package org.cherrypic.domain.album.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import org.cherrypic.album.enums.AlbumType;
import org.cherrypic.global.util.StorageUnitConverter;

public record AlbumInfoResponse(
        @Schema(description = "앨범 제목", example = "맛집 탐방") String title,
        @Schema(description = "앨범 커버 사진 url", example = "https://example.jpg") String coverUrl,
        @Schema(description = "앨범 유형", example = "PRO") AlbumType type,
        @Schema(description = "사용한 용량 (GB)", example = "2.4")
                @JsonFormat(shape = JsonFormat.Shape.STRING)
                BigDecimal capacityUsedGb,
        @Schema(description = "총 용량 (GB)", example = "3") BigDecimal totalCapacityGb,
        @Schema(description = "앨범 Host의 이름", example = "김OO") String hostName,
        @Schema(description = "앨범의 참여자 수", example = "6") Integer numOfParticipants) {
    public static AlbumInfoResponse of(
            String title,
            String coverUrl,
            AlbumType type,
            BigDecimal capacityUsedMb,
            BigDecimal totalCapacityMb,
            String hostName,
            Integer numOfParticipants) {
        return new AlbumInfoResponse(
                title,
                coverUrl,
                type,
                StorageUnitConverter.mbToGb(capacityUsedMb),
                StorageUnitConverter.mbToGb(totalCapacityMb),
                hostName,
                numOfParticipants);
    }
}
