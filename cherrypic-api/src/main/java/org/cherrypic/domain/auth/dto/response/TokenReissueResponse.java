package org.cherrypic.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenReissueResponse(
        @Schema(description = "Access Token") String accessToken,
        @Schema(description = "Refresh Token") String refreshToken) {
    public static TokenReissueResponse of(String accessToken, String refreshToken) {
        return new TokenReissueResponse(accessToken, refreshToken);
    }
}
