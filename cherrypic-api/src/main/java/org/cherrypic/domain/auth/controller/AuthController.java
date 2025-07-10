package org.cherrypic.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.auth.dto.request.IdTokenRequest;
import org.cherrypic.domain.auth.dto.response.SocialLoginResponse;
import org.cherrypic.domain.auth.entity.OauthProvider;
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
}
