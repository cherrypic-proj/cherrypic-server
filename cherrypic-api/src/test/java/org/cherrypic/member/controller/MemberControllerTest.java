package org.cherrypic.member.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.cherrypic.album.enums.ParticipationAction;
import org.cherrypic.domain.member.controller.MemberController;
import org.cherrypic.domain.member.dto.request.FcmTokenSaveRequest;
import org.cherrypic.domain.member.dto.request.MemberProfileUpdateRequest;
import org.cherrypic.domain.member.dto.response.LocalImageDeletionToggleResponse;
import org.cherrypic.domain.member.dto.response.MemberInfoResponse;
import org.cherrypic.domain.member.dto.response.MemberProfileUpdateResponse;
import org.cherrypic.domain.member.dto.response.ParticipationHistoryResponse;
import org.cherrypic.domain.member.service.MemberService;
import org.cherrypic.domain.member.service.ParticipationHistoryQueryService;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;
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
    @MockitoBean private ParticipationHistoryQueryService participationHistoryQueryService;

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
                            false);

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
                    .andExpect(jsonPath("$.data.localImageDeletion").value(false));
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
    class 로컬_이미지_삭제_허용_여부_변경_요청_시 {

        @Test
        void 유효한_요청이면_로컬_이미지_삭제_허용_여부를_변경한다() throws Exception {
            // given
            LocalImageDeletionToggleResponse response = new LocalImageDeletionToggleResponse(true);

            given(memberService.toggleLocalImageDeletion()).willReturn(response);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/members/me/local-image-deletion")
                                    .contentType(MediaType.APPLICATION_JSON));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.localImageDeletion").value(true));
        }
    }

    @Nested
    class 앨범의_참여_이력_조회_요청_시 {

        @Test
        void 정렬_조건이_ASC이면_historyId를_오름차순으로_응답한다() throws Exception {
            // given
            List<ParticipationHistoryResponse> responses =
                    List.of(
                            new ParticipationHistoryResponse(
                                    1L,
                                    "testTitle1",
                                    ParticipationAction.JOIN,
                                    LocalDateTime.of(2025, 8, 1, 0, 0)),
                            new ParticipationHistoryResponse(
                                    2L,
                                    "testTitle2",
                                    ParticipationAction.LEAVE,
                                    LocalDateTime.of(2025, 8, 2, 0, 0)),
                            new ParticipationHistoryResponse(
                                    3L,
                                    "testTitle3",
                                    ParticipationAction.KICK,
                                    LocalDateTime.of(2025, 8, 3, 0, 0)));

            given(
                            participationHistoryQueryService.getParticipationHistory(
                                    null, 3, SortDirection.ASC))
                    .willReturn(new SliceResponse<>(responses, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/members/me/participation-history")
                                    .param("size", "3")
                                    .param("direction", "ASC"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].historyId").value(1))
                    .andExpect(jsonPath("$.data.content[1].historyId").value(2))
                    .andExpect(jsonPath("$.data.content[2].historyId").value(3))
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        @Test
        void 정렬_조건이_DESC이면_historyId를_내림차순으로_응답한다() throws Exception {
            // given
            List<ParticipationHistoryResponse> responses =
                    List.of(
                            new ParticipationHistoryResponse(
                                    3L,
                                    "testTitle3",
                                    ParticipationAction.KICK,
                                    LocalDateTime.of(2025, 8, 3, 0, 0)),
                            new ParticipationHistoryResponse(
                                    2L,
                                    "testTitle2",
                                    ParticipationAction.LEAVE,
                                    LocalDateTime.of(2025, 8, 2, 0, 0)),
                            new ParticipationHistoryResponse(
                                    1L,
                                    "testTitle1",
                                    ParticipationAction.JOIN,
                                    LocalDateTime.of(2025, 8, 1, 0, 0)));

            given(
                            participationHistoryQueryService.getParticipationHistory(
                                    null, 3, SortDirection.DESC))
                    .willReturn(new SliceResponse<>(responses, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(get("/members/me/participation-history").param("size", "3"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].historyId").value(3))
                    .andExpect(jsonPath("$.data.content[1].historyId").value(2))
                    .andExpect(jsonPath("$.data.content[2].historyId").value(1))
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        @Test
        void 마지막_페이지인_경우_isLast를_true로_응답한다() throws Exception {
            // given
            List<ParticipationHistoryResponse> responses =
                    List.of(
                            new ParticipationHistoryResponse(
                                    1L,
                                    "testTitle1",
                                    ParticipationAction.JOIN,
                                    LocalDateTime.of(2025, 8, 1, 0, 0)));

            given(
                            participationHistoryQueryService.getParticipationHistory(
                                    null, 1, SortDirection.DESC))
                    .willReturn(new SliceResponse<>(responses, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(get("/members/me/participation-history").param("size", "1"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].historyId").value(1))
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        @Test
        void 마지막_페이지가_아닌_경우_isLast를_false로_응답한다() throws Exception {
            // given
            List<ParticipationHistoryResponse> responses =
                    List.of(
                            new ParticipationHistoryResponse(
                                    2L,
                                    "testTitle2",
                                    ParticipationAction.LEAVE,
                                    LocalDateTime.of(2025, 8, 2, 0, 0)),
                            new ParticipationHistoryResponse(
                                    1L,
                                    "testTitle1",
                                    ParticipationAction.JOIN,
                                    LocalDateTime.of(2025, 8, 1, 0, 0)));

            given(
                            participationHistoryQueryService.getParticipationHistory(
                                    null, 1, SortDirection.DESC))
                    .willReturn(new SliceResponse<>(responses, false));

            // when & then
            ResultActions perform =
                    mockMvc.perform(get("/members/me/participation-history").param("size", "1"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].historyId").value(2))
                    .andExpect(jsonPath("$.data.isLast").value(false));
        }

        @ParameterizedTest
        @ValueSource(strings = {"-1", "-999", "0"})
        void 페이지_크기를_0_이하로_설정하면_예외가_발생한다(String pageSize) throws Exception {
            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/members/me/participation-history").param("size", pageSize));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("ConstraintViolationException"))
                    .andExpect(jsonPath("$.data.message").value("페이지 크기는 0보다 큰 값만 가능합니다."));
        }

        @ParameterizedTest
        @ValueSource(strings = {"ASCC", "DESCC", "OLDEST", "NEWEST"})
        void 존재하지_않는_정렬_기준을_입력한_경우_예외가_발생한다(String sort) throws Exception {
            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/members/me/participation-history")
                                    .param("size", "2")
                                    .param("direction", sort));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("METHOD_ARGUMENT_TYPE_MISMATCH"))
                    .andExpect(jsonPath("$.data.message").value("요청한 값의 타입이 잘못되어 처리할 수 없습니다."));
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
}
