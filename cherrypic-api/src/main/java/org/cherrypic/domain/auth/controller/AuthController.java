package org.cherrypic.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.auth.dto.request.IdTokenRequest;
import org.cherrypic.domain.auth.dto.response.SocialLoginResponse;
import org.cherrypic.domain.auth.dto.response.TokenReissueResponse;
import org.cherrypic.domain.auth.enums.OauthProvider;
import org.cherrypic.domain.auth.service.AuthService;
import org.cherrypic.domain.auth.util.CookieUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "1-1. 인증 API", description = "인증 관련 API입니다.")
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;

    @PostMapping("/social-login")
    @Operation(summary = "소셜 로그인", description = "소셜 로그인을 진행합니다.")
    public ResponseEntity<Void> memberSocialLogin(
            @RequestParam(name = "oauthProvider") OauthProvider provider,
            @Valid @RequestBody IdTokenRequest request) {
        SocialLoginResponse response = authService.socialLoginMember(provider, request);

        HttpHeaders headers =
                cookieUtil.generateTokenCookies(response.accessToken(), response.refreshToken());

        return ResponseEntity.noContent().headers(headers).build();
    }

    @Operation(
            summary = "토큰 재발급",
            description = "엑세스 토큰이 만료되었을 경우, 리프레시 토큰을 이용해 엑세스 및 리프레시 토큰을 재발급합니다.")
    @PostMapping("/reissue")
    public ResponseEntity<Void> tokenReissue(
            @CookieValue("refreshToken") String refreshTokenValue) {
        TokenReissueResponse response = authService.reissueToken(refreshTokenValue);

        HttpHeaders headers =
                cookieUtil.generateTokenCookies(response.accessToken(), response.refreshToken());

        return ResponseEntity.noContent().headers(headers).build();
    }

    @Operation(
            summary = "회원 로그아웃",
            description = "로그아웃 시, 쿠키에 저장된 accessToken과 refreshToken이 만료 처리됩니다.")
    @PostMapping("/logout")
    public ResponseEntity<Void> memberLogout() {
        authService.logoutMember();

        return ResponseEntity.noContent().headers(cookieUtil.deleteTokenCookies()).build();
    }
}
