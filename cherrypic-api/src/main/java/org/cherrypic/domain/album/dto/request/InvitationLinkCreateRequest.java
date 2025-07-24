package org.cherrypic.domain.album.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record InvitationLinkCreateRequest(
        @NotBlank(message = "앨범 ID는 비워둘 수 없습니다")
                @Schema(description = "초대 링크를 생성할 앨범의 ID", example = "1")
                Long albumId) {}
