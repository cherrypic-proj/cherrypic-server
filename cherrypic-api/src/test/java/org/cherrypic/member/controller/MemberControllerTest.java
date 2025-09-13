package org.cherrypic.member.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cherrypic.domain.member.controller.MemberController;
import org.cherrypic.domain.member.dto.request.FcmTokenSaveRequest;
import org.cherrypic.domain.member.dto.request.MemberProfileUpdateRequest;
import org.cherrypic.domain.member.dto.response.MarketingAgreeToggleResponse;
import org.cherrypic.domain.member.dto.response.MemberInfoResponse;
import org.cherrypic.domain.member.dto.response.MemberProfileUpdateResponse;
import org.cherrypic.domain.member.dto.response.ServiceAlarmAgreeToggleResponse;
import org.cherrypic.domain.member.service.MemberService;
import org.cherrypic.member.enums.MemberRole;
import org.cherrypic.member.enums.MemberStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(MemberController.class)
@AutoConfigureMockMvc(addFilters = false)
class MemberControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private MemberService memberService;

    @Nested
    class 회원_정보_조회_요청_시 {

        @Test
        void 유효한_요청이면_회원_정보를_조회한다() throws Exception {
            // given
            MemberInfoResponse response =
                    new MemberInfoResponse(
                            1L,
                            "https://kauth.kakao.com",
                            "testNickname",
                            "testProfileImageUrl",
                            MemberStatus.NORMAL,
                            MemberRole.USER,
                            Boolean.FALSE,
                            Boolean.FALSE);

            given(memberService.getMemberInfo()).willReturn(response);

            // when & then
            ResultActions perform = mockMvc.perform(get("/members/me"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.memberId").value(1))
                    .andExpect(jsonPath("$.data.oauthProvider").value("https://kauth.kakao.com"))
                    .andExpect(jsonPath("$.data.nickname").value("testNickname"))
                    .andExpect(jsonPath("$.data.profileImageUrl").value("testProfileImageUrl"))
                    .andExpect(jsonPath("$.data.status").value("NORMAL"))
                    .andExpect(jsonPath("$.data.role").value("USER"))
                    .andExpect(jsonPath("$.data.serviceAlarmAgree").value("false"))
                    .andExpect(jsonPath("$.data.marketingAgree").value("false"));
        }
    }

    @Nested
    class 회원_프로필_수정_요청_시 {

        @Test
        void 유효한_요청이면_변경된_회원_닉네임과_프로필_이미지를_반환한다() throws Exception {
            // given
            MemberProfileUpdateRequest request =
                    new MemberProfileUpdateRequest("updateNickname", "updateProfileImageUrl");
            MemberProfileUpdateResponse response =
                    new MemberProfileUpdateResponse("updateNickname", "updateProfileImageUrl");

            given(memberService.updateProfile(request)).willReturn(response);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/members/me")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.nickname").value("updateNickname"))
                    .andExpect(jsonPath("$.data.profileImageUrl").value("updateProfileImageUrl"));
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {" "})
        void 닉네임이_null_또는_공백이면_예외가_발생한다(String nickname) throws Exception {
            // given
            MemberProfileUpdateRequest request = new MemberProfileUpdateRequest(nickname, null);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/members/me")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("닉네임은 비워둘 수 없습니다."));
        }

        @Test
        void 닉네임에_특수문자를_포함하면_예외가_발생한다() throws Exception {
            // given
            MemberProfileUpdateRequest request = new MemberProfileUpdateRequest("닉!네@임#수^정", null);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/members/me")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("닉네임에는 특수문자를 포함할 수 없습니다."));
        }

        @Test
        void 닉네임이_15자를_초과하면_예외가_발생한다() throws Exception {
            // given
            MemberProfileUpdateRequest request =
                    new MemberProfileUpdateRequest("t".repeat(16), null);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/members/me")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("닉네임은 최대 15자까지 입력 가능합니다."));
        }
    }

    @Nested
    class FCM_토큰_저장_요청_시 {

        @Test
        void 유효한_요청이면_FCM_토큰을_저장하고_NO_CONTENT로_반환한다() throws Exception {
            // given
            FcmTokenSaveRequest request = new FcmTokenSaveRequest("testFcmToken");

            willDoNothing().given(memberService).saveFcmToken(request);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/members/fcm-tokens")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isNoContent())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NO_CONTENT.value()));
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        @ValueSource(strings = {" "})
        void FCM_토큰이_null_또는_공백이면_예외가_발생한다(String fcmToken) throws Exception {
            // given
            FcmTokenSaveRequest request = new FcmTokenSaveRequest(fcmToken);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/members/fcm-tokens")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("FCM Token은 비워둘 수 없습니다."));
        }
    }

    @Nested
    class 서비스_알림_수신_동의_상태_변경_요청_시 {

        @Test
        void 유효한_요청이면_서비스_알림_수신_동의_상태를_변경하고_반환한다() throws Exception {
            // given
            ServiceAlarmAgreeToggleResponse response =
                    new ServiceAlarmAgreeToggleResponse(Boolean.TRUE);

            given(memberService.toggleServiceAlarmAgree()).willReturn(response);

            // when & then
            ResultActions perform = mockMvc.perform(patch("/members/me/service-alarm"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.serviceAlarmAgree").value("true"));
        }
    }

    @Nested
    class 마케팅_수신_동의_상태_변경_요청_시 {

        @Test
        void 유효한_요청이면_마케팅_수신_동의_상태를_변경하고_반환한다() throws Exception {
            // given
            MarketingAgreeToggleResponse response = new MarketingAgreeToggleResponse(Boolean.TRUE);

            given(memberService.toggleMarketingAgree()).willReturn(response);

            // when & then
            ResultActions perform = mockMvc.perform(patch("/members/me/marketing"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.marketingAgree").value("true"));
        }
    }
}
