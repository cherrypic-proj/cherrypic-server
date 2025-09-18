package org.cherrypic.domain.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cherrypic.member.entity.Member;

public record ServiceAlarmAgreeToggleResponse(
        @Schema(description = "서비스 알림 수신 동의 여부", example = "false") Boolean serviceAlarmAgree) {
    public static ServiceAlarmAgreeToggleResponse from(Member member) {
        return new ServiceAlarmAgreeToggleResponse(member.getServiceAlarmAgree());
    }
}
