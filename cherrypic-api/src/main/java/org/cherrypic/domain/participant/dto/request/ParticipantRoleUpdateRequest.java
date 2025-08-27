package org.cherrypic.domain.participant.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cherrypic.global.annotation.Enum;
import org.cherrypic.participant.enums.ParticipantRole;

public record ParticipantRoleUpdateRequest(
        @Enum(message = "참가자 권한은 비워둘 수 없으며, HOST, STANDARD, LIMITED만 지원됩니다.")
                @Schema(description = "참가자 권한", example = "STANDARD")
                ParticipantRole role) {}
