package org.cherrypic.domain.auth.dto;

public record RefreshTokenDto(Long memberId, String tokenValue, Long ttl) {
    public static RefreshTokenDto of(Long memberId, String tokenValue, Long ttl) {
        return new RefreshTokenDto(memberId, tokenValue, ttl);
    }
}
