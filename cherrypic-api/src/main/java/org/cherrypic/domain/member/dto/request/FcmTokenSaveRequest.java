package org.cherrypic.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record FcmTokenSaveRequest(
        @NotBlank(message = "FCM Token은 비워둘 수 없습니다.")
                @Schema(description = "FCM 토큰", defaultValue = "FCM Token")
                String fcmToken) {}
