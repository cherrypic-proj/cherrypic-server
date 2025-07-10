package org.cherrypic.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.cherrypic.auth.entity.RefreshToken;
import org.cherrypic.auth.repository.RefreshTokenRepository;
import org.cherrypic.domain.auth.util.JwtUtil;
import org.cherrypic.member.enums.MemberRole;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    public String createAccessToken(Long memberId, MemberRole memberRole) {
        return jwtUtil.generateAccessToken(memberId, memberRole);
    }

    public String createRefreshToken(Long memberId) {
        String token = jwtUtil.generateRefreshToken(memberId);
        RefreshToken refreshToken =
                RefreshToken.builder()
                        .memberId(memberId)
                        .token(token)
                        .ttl(jwtUtil.getRefreshTokenExpirationTime())
                        .build();
        refreshTokenRepository.save(refreshToken);

        return token;
    }
}
