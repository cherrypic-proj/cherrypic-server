package org.cherrypic.domain.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cherrypic.member.entity.Member;

public record MarketingAgreeToggleResponse(
        @Schema(description = "마케팅 수신 동의 여부", example = "false") Boolean marketingAgree) {
    public static MarketingAgreeToggleResponse from(Member member) {
        return new MarketingAgreeToggleResponse(member.getMarketingAgree());
    }
}
