package org.cherrypic.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.cherrypic.IntegrationTest;
import org.cherrypic.auth.entity.RefreshToken;
import org.cherrypic.domain.auth.dto.AccessTokenDto;
import org.cherrypic.domain.auth.dto.RefreshTokenDto;
import org.cherrypic.domain.auth.dto.request.IdTokenRequest;
import org.cherrypic.domain.auth.dto.response.SocialLoginResponse;
import org.cherrypic.domain.auth.dto.response.TokenReissueResponse;
import org.cherrypic.domain.auth.enums.OauthProvider;
import org.cherrypic.domain.auth.exception.AuthErrorCode;
import org.cherrypic.domain.auth.exception.AuthException;
import org.cherrypic.domain.auth.repository.RefreshTokenRepository;
import org.cherrypic.domain.auth.service.AuthService;
import org.cherrypic.domain.auth.service.IdTokenVerifier;
import org.cherrypic.domain.auth.service.JwtTokenService;
import org.cherrypic.domain.member.repository.MemberRepository;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.member.entity.Member;
import org.cherrypic.member.entity.OauthInfo;
import org.cherrypic.member.enums.MemberRole;
import org.cherrypic.member.enums.MemberStatus;
import org.junit.jupiter.api.BeforeEach;
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
    @Autowired private RefreshTokenRepository refreshTokenRepository;

    @MockitoBean private JwtTokenService jwtTokenService;
    @MockitoBean private IdTokenVerifier idTokenVerifier;

    @MockitoBean private MemberUtil memberUtil;

    @Nested
    class 소셜_로그인할_때 {

        @Test
        void 유효한_ID_토큰이면_엑세스와_리프레시_토큰을_반환한다() {
            // given
            given(idTokenVerifier.getOidcUser(anyString(), any()))
                    .willReturn(mockOidcUser("kakao-sub-001", "https://kauth.kakao.com"));
            given(jwtTokenService.createAccessToken(anyLong(), any(MemberRole.class)))
                    .willReturn("fake-access-token");
            given(jwtTokenService.createRefreshToken(anyLong())).willReturn("fake-refresh-token");

            IdTokenRequest request = new IdTokenRequest("testIdToken");

            // when
            SocialLoginResponse response =
                    authService.socialLoginMember(OauthProvider.KAKAO, request);

            // then
            assertThat(response)
                    .extracting("accessToken", "refreshToken")
                    .containsExactly("fake-access-token", "fake-refresh-token");
        }

        @Test
        void 처음_로그인하는_회원이면_회원_정보를_저장한다() {
            // given
            given(idTokenVerifier.getOidcUser(anyString(), any()))
                    .willReturn(mockOidcUser("apple-sub-001", "https://appleid.apple.com"));
            given(jwtTokenService.createAccessToken(anyLong(), any(MemberRole.class)))
                    .willReturn("fake-access-token");
            given(jwtTokenService.createRefreshToken(anyLong())).willReturn("fake-refresh-token");

            IdTokenRequest request = new IdTokenRequest("testIdToken");

            // when
            authService.socialLoginMember(OauthProvider.KAKAO, request);

            // then
            Member member = memberRepository.findById(1L).get();
            assertThat(member)
                    .extracting("id", "role", "status")
                    .containsExactly(1L, MemberRole.USER, MemberStatus.NORMAL);
        }

        @Test
        void ID_토큰_검증에_실패하면_예외가_발생한다() {
            // given
            given(idTokenVerifier.getOidcUser(anyString(), any()))
                    .willThrow(new AuthException(AuthErrorCode.ID_TOKEN_VERIFICATION_FAILED));

            IdTokenRequest request = new IdTokenRequest("invalidIdToken");

            // when & then
            assertThatThrownBy(() -> authService.socialLoginMember(OauthProvider.KAKAO, request))
                    .isInstanceOf(AuthException.class)
                    .hasMessage(AuthErrorCode.ID_TOKEN_VERIFICATION_FAILED.getMessage());
        }

        private OidcUser mockOidcUser(String sub, String iss) {
            OidcIdToken idToken =
                    new OidcIdToken(
                            "fake-id-token",
                            Instant.now(),
                            Instant.now().plusSeconds(3600),
                            Map.of("sub", sub, "iss", iss));

            return new DefaultOidcUser(List.of(), idToken);
        }
    }

    @Nested
    class 토큰_재발급할_때 {

        @BeforeEach
        void setUp() {
            memberRepository.save(
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"),
                            "testNickname",
                            "testProfileImageUrl"));
        }

        @Test
        void 유효한_리프레시_토큰이면_새로운_엑세스_토큰과_리프레시_토큰을_반환한다() {
            // given
            RefreshTokenDto oldRefreshTokenDto =
                    RefreshTokenDto.of(1L, "fake-old-register-token", 604800L);
            RefreshTokenDto newRefreshTokenDto =
                    RefreshTokenDto.of(1L, "fake-new-refresh-token", 604800L);
            AccessTokenDto newAccessTokenDto =
                    AccessTokenDto.of(1L, MemberRole.USER, "fake-new-access-token");

            given(jwtTokenService.retrieveRefreshToken(anyString())).willReturn(oldRefreshTokenDto);
            given(jwtTokenService.reissueRefreshToken(oldRefreshTokenDto))
                    .willReturn(newRefreshTokenDto);
            given(jwtTokenService.reissueAccessToken(any())).willReturn(newAccessTokenDto);

            // when
            TokenReissueResponse response = authService.reissueToken("testRefreshToken");

            // then
            assertThat(response)
                    .extracting("accessToken", "refreshToken")
                    .containsExactly("fake-new-access-token", "fake-new-refresh-token");
        }

        @Test
        void 만료된_리프레시_토큰이면_예외가_발생한다() {
            // given
            assertThatThrownBy(() -> authService.reissueToken("testRefreshToken"))
                    .isInstanceOf(AuthException.class)
                    .hasMessage(AuthErrorCode.INVALID_REFRESH_TOKEN.getMessage());
            verify(jwtTokenService, times(1)).retrieveRefreshToken(anyString());
        }
    }

    @Nested
    class 로그아웃할_때 {

        @BeforeEach
        void setUp() {
            Member member =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"),
                            "testNickname",
                            "testProfileImageUrl");
            memberRepository.save(member);

            given(memberUtil.getCurrentMember()).willReturn(member);
        }

        @Test
        void Redis에_저장된_리프레시_토큰이_삭제된다() {
            // given
            RefreshToken refreshToken =
                    RefreshToken.builder().memberId(1L).token("testRefreshToken").build();
            refreshTokenRepository.save(refreshToken);

            // when
            authService.logoutMember();

            // then
            assertThat(refreshTokenRepository.findById(1L).isEmpty()).isTrue();
        }
    }
}
