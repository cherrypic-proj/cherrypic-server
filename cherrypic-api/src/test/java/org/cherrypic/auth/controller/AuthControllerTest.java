package org.cherrypic.auth.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.cherrypic.domain.auth.controller.AuthController;
import org.cherrypic.domain.auth.dto.request.IdTokenRequest;
import org.cherrypic.domain.auth.dto.response.SocialLoginResponse;
import org.cherrypic.domain.auth.dto.response.TokenReissueResponse;
import org.cherrypic.domain.auth.enums.OauthProvider;
import org.cherrypic.domain.auth.exception.AuthErrorCode;
import org.cherrypic.domain.auth.exception.AuthException;
import org.cherrypic.domain.auth.service.AuthService;
import org.cherrypic.domain.auth.util.CookieUtil;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AuthService authService;

    @MockitoBean private CookieUtil cookieUtil;

    @Nested
    class 소셜_로그인할_때 {

        @Test
        void 유효한_ID_토큰이면_소셜_로그인에_성공한다() throws Exception {
            // given
            IdTokenRequest request = new IdTokenRequest("testIdTokenValue");

            SocialLoginResponse response =
                    new SocialLoginResponse("fake-access-token", "fake-refresh-token");

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.SET_COOKIE, "accessToken=fake-access-token");
            headers.add(HttpHeaders.SET_COOKIE, "refreshToken=fake-refresh-token");

            given(authService.socialLoginMember(eq(OauthProvider.KAKAO), any()))
                    .willReturn(response);
            given(cookieUtil.generateTokenCookies(response.accessToken(), response.refreshToken()))
                    .willReturn(headers);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/auth/social-login")
                                    .param("oauthProvider", "KAKAO")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isNoContent())
                    .andExpect(cookie().exists("accessToken"))
                    .andExpect(cookie().exists("refreshToken"));
        }

        @Test
        void ID_토큰이_blank이면_예외가_발생한다() throws Exception {
            // given
            IdTokenRequest request = new IdTokenRequest("");

            SocialLoginResponse response =
                    new SocialLoginResponse("fake-access-token", "fake-refresh-token");

            given(authService.socialLoginMember(eq(OauthProvider.KAKAO), any()))
                    .willReturn(response);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/auth/social-login")
                                    .param("oauthProvider", "KAKAO")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("Id Token은 비워둘 수 없습니다."));
        }
    }

    @Nested
    class 토큰_재발급할_때 {

        @Test
        void 유효한_리프레시_토큰이면_새로운_토큰을_반환한다() throws Exception {
            // given
            TokenReissueResponse response =
                    TokenReissueResponse.of("fake-access-token", "fake-refresh-token");

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.SET_COOKIE, "accessToken=fake-access-token");
            headers.add(HttpHeaders.SET_COOKIE, "refreshToken=fake-refresh-token");

            given(authService.reissueToken(anyString())).willReturn(response);
            given(cookieUtil.generateTokenCookies(response.accessToken(), response.refreshToken()))
                    .willReturn(headers);

            Cookie refreshTokenCookie = new Cookie("refreshToken", "testRefreshTokenValue");

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/auth/reissue")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .cookie(refreshTokenCookie));

            perform.andExpect(status().isNoContent())
                    .andExpect(cookie().exists("accessToken"))
                    .andExpect(cookie().exists("refreshToken"));
        }

        @Test
        void 만료된_리프레시_토큰이면_예외가_발생한다() throws Exception {
            // given
            given(authService.reissueToken(anyString()))
                    .willThrow(new AuthException(AuthErrorCode.EXPIRED_REFRESH_TOKEN));

            Cookie refreshTokenCookie = new Cookie("refreshToken", "invalidRefreshToken");

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/auth/reissue")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .cookie(refreshTokenCookie));

            perform.andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.UNAUTHORIZED.value()))
                    .andExpect(
                            jsonPath("$.data.code")
                                    .value(AuthErrorCode.EXPIRED_REFRESH_TOKEN.name()))
                    .andExpect(
                            jsonPath("$.data.message")
                                    .value(AuthErrorCode.EXPIRED_REFRESH_TOKEN.getMessage()));
        }
    }
}
