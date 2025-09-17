package org.cherrypic.domain.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cherrypic.member.entity.Member;

public record LocalImageDeletionToggleResponse(
        @Schema(description = "로컬 이미지 삭제 관련 토글 상태", example = "true") Boolean localImageDeletion) {
    public static LocalImageDeletionToggleResponse from(Member member) {
        return new LocalImageDeletionToggleResponse(member.getLocalImageDeletion());
    }
}
