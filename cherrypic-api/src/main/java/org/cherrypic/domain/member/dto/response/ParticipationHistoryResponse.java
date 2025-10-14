package org.cherrypic.domain.member.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import org.cherrypic.album.enums.ParticipationAction;

public record ParticipationHistoryResponse(
        @Schema(description = "앨범 참여 이력 ID", example = "1") Long historyId,
        @Schema(description = "앨범 이름 스냅샷 (액션 발생 직전 이름 기준)", example = "가족여행") String albumTitle,
        @Schema(description = "액션 타입", example = "JOIN") ParticipationAction action,
        @JsonFormat(
                        shape = JsonFormat.Shape.STRING,
                        pattern = "yyyy-MM-dd",
                        timezone = "Asia/Seoul")
                @Schema(description = "액션 발생일", example = "2025-08-01")
                LocalDateTime eventTime) {}
