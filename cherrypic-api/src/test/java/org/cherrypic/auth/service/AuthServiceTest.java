package org.cherrypic.auth.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.cherrypic.IntegrationTest;
import org.cherrypic.domain.auth.dto.request.IdTokenRequest;
import org.cherrypic.domain.auth.dto.response.SocialLoginResponse;
import org.cherrypic.domain.auth.entity.OauthProvider;
import org.cherrypic.domain.auth.service.AuthService;
import org.cherrypic.domain.auth.service.IdTokenVerifier;
import org.cherrypic.domain.auth.service.JwtTokenService;
import org.cherrypic.member.entity.Member;
import org.cherrypic.member.enums.MemberRole;
import org.cherrypic.member.enums.MemberStatus;
import org.cherrypic.member.repository.MemberRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public class AuthServiceTest extends IntegrationTest {

    @Autowired private AuthService authService;
    @Autowired private MemberRepository memberRepository;

    @MockitoBean private JwtTokenService jwtTokenService;
    @MockitoBean private IdTokenVerifier idTokenVerifier;

    @Nested
    class 소셜_로그인할_때 {

        @Test
        void 유효한_ID_토큰이면_소셜_로그인에_성공한다() {
            // given
            given(idTokenVerifier.getOidcUser(anyString(), any())).willReturn(mockOidcUser());
            given(jwtTokenService.createAccessToken(anyLong(), any(MemberRole.class)))
                    .willReturn("fake-access-token");
            given(jwtTokenService.createRefreshToken(anyLong())).willReturn("fake-refresh-token");

            IdTokenRequest request = new IdTokenRequest("testIdTokenValue");

            // when
            SocialLoginResponse response =
                    authService.socialLoginMember(OauthProvider.KAKAO, request);

            // then
            Assertions.assertAll(
                    () -> assertThat(response.accessToken()).isEqualTo("fake-access-token"),
                    () -> assertThat(response.refreshToken()).isEqualTo("fake-refresh-token"));
        }

        @Test
        void 처음_로그인하는_회원이면_회원_정보를_저장한다() {
            // given
            given(idTokenVerifier.getOidcUser(anyString(), any())).willReturn(mockOidcUser());
            given(jwtTokenService.createAccessToken(anyLong(), any(MemberRole.class)))
                    .willReturn("fake-access-token");
            given(jwtTokenService.createRefreshToken(anyLong())).willReturn("fake-refresh-token");

            IdTokenRequest request = new IdTokenRequest("testIdTokenValue");

            // when
            authService.socialLoginMember(OauthProvider.KAKAO, request);

            // then
            Member member = memberRepository.findById(1L).get();
            Assertions.assertAll(
                    () -> assertThat(member.getId()).isEqualTo(1L),
                    () -> assertThat(member.getNickname()).isNotNull(),
                    () -> assertThat(member.getRole()).isEqualTo(MemberRole.USER),
                    () -> assertThat(member.getStatus()).isEqualTo(MemberStatus.NORMAL));
        }

        private OidcUser mockOidcUser() {
            OidcIdToken idToken =
                    new OidcIdToken(
                            "fake-id-token",
                            Instant.now(),
                            Instant.now().plusSeconds(3600),
                            Map.of("sub", "testOauthId", "iss", "https://test.oauth.provider"));

            return new DefaultOidcUser(List.of(), idToken);
        }
    }
}
