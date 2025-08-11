package org.cherrypic.member.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cherrypic.domain.member.controller.MemberController;
import org.cherrypic.domain.member.dto.request.FcmTokenSaveRequest;
import org.cherrypic.domain.member.dto.response.MemberInfoResponse;
import org.cherrypic.domain.member.service.MemberService;
import org.cherrypic.member.enums.MemberRole;
import org.cherrypic.member.enums.MemberStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
                            MemberRole.USER);

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
                    .andExpect(jsonPath("$.data.role").value("USER"));
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
    }
}
