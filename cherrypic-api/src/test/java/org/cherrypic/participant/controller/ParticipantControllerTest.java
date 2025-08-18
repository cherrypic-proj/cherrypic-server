package org.cherrypic.participant.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.cherrypic.domain.album.dto.response.*;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.participant.controller.ParticipantController;
import org.cherrypic.domain.participant.dto.response.ParticipantListResponse;
import org.cherrypic.domain.participant.exception.ParticipantErrorCode;
import org.cherrypic.domain.participant.service.ParticipantService;
import org.cherrypic.exception.CustomException;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.participant.enums.ParticipantRole;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(ParticipantController.class)
@AutoConfigureMockMvc(addFilters = false)
class ParticipantControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ParticipantService participantService;

    @Nested
    class 앨범_나가기_요청_시 {

        @Test
        void 유효한_요청이면_참가자를_앨범에서_삭제하고_NO_CONTENT_로_반환한다() throws Exception {
            // given
            willDoNothing().given(participantService).leaveAlbum(1L);

            // when & then
            ResultActions perform = mockMvc.perform(delete("/albums/1/participants/me"));

            perform.andExpect(status().isNoContent())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NO_CONTENT.value()));
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() throws Exception {
            // given
            willThrow(new CustomException(AlbumErrorCode.ALBUM_NOT_FOUND))
                    .given(participantService)
                    .leaveAlbum(1L);

            // when & then
            ResultActions perform = mockMvc.perform(delete("/albums/1/participants/me"));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("ALBUM_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("앨범이 존재하지 않습니다."));
        }

        @Test
        void 앨범_참가자가_아닌_경우_예외가_발생한다() throws Exception {
            // given
            willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT))
                    .given(participantService)
                    .leaveAlbum(1L);

            // when & then
            ResultActions perform = mockMvc.perform(delete("/albums/1/participants/me"));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_ALBUM_PARTICIPANT"))
                    .andExpect(jsonPath("$.data.message").value("앨범에 속하지 않은 사용자입니다."));
        }

        @Test
        void 앨범_방장인_경우_예외가_발생한다() throws Exception {
            // given
            willThrow(new CustomException(AlbumErrorCode.HOST_LEAVE_NOT_ALLOWED))
                    .given(participantService)
                    .leaveAlbum(1L);

            // when & then
            ResultActions perform = mockMvc.perform(delete("/albums/1/participants/me"));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("HOST_LEAVE_NOT_ALLOWED"))
                    .andExpect(jsonPath("$.data.message").value("방장은 앨범을 나갈 수 없습니다."));
        }
    }

    @Nested
    class 참가자_강퇴_요청_시 {

        @Test
        void 유효한_요청이면_앨범_방장이_참가자를_강퇴하여_앨범에서_제거하고_NO_CONTENT_로_반환한다() throws Exception {
            // given
            willDoNothing().given(participantService).kickParticipant(1L, 1L);

            // when & then
            ResultActions perform = mockMvc.perform(delete("/albums/1/participants/1"));

            perform.andExpect(status().isNoContent())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NO_CONTENT.value()));
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() throws Exception {
            // given
            willThrow(new CustomException(AlbumErrorCode.ALBUM_NOT_FOUND))
                    .given(participantService)
                    .kickParticipant(1L, 1L);

            // when & then
            ResultActions perform = mockMvc.perform(delete("/albums/1/participants/1"));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("ALBUM_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("앨범이 존재하지 않습니다."));
        }

        @Test
        void 강퇴_요청자가_앨범_참가자가_아닌_경우_예외가_발생한다() throws Exception {
            // given
            willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT))
                    .given(participantService)
                    .kickParticipant(1L, 1L);

            // when & then
            ResultActions perform = mockMvc.perform(delete("/albums/1/participants/1"));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_ALBUM_PARTICIPANT"))
                    .andExpect(jsonPath("$.data.message").value("앨범에 속하지 않은 사용자입니다."));
        }

        @Test
        void 강퇴_요청자가_앨범_방장이_아닌_경우_예외가_발생한다() throws Exception {
            // given
            willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_HOST))
                    .given(participantService)
                    .kickParticipant(1L, 1L);

            // when & then
            ResultActions perform = mockMvc.perform(delete("/albums/1/participants/1"));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_ALBUM_HOST"))
                    .andExpect(jsonPath("$.data.message").value("방장이 아닌 경우 권한이 없습니다."));
        }

        @Test
        void 앨범_방장이_자기_자신을_강퇴하려는_경우_예외가_발생한다() throws Exception {
            // given
            willThrow(new CustomException(AlbumErrorCode.HOST_SELF_KICK_NOT_ALLOWED))
                    .given(participantService)
                    .kickParticipant(1L, 1L);

            // when & then
            ResultActions perform = mockMvc.perform(delete("/albums/1/participants/1"));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("HOST_SELF_KICK_NOT_ALLOWED"))
                    .andExpect(jsonPath("$.data.message").value("방장은 자기 자신을 강퇴할 수 없습니다."));
        }

        @Test
        void 강퇴_대상_참가자가_존재하지_않는_경우_예외가_발생한다() throws Exception {
            // given
            willThrow(new CustomException(ParticipantErrorCode.PARTICIPANT_NOT_FOUND))
                    .given(participantService)
                    .kickParticipant(1L, 1L);

            // when & then
            ResultActions perform = mockMvc.perform(delete("/albums/1/participants/1"));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("PARTICIPANT_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("참가자를 찾을 수 없습니다."));
        }

        @Test
        void 강퇴_대상이_앨범_참가자가_아닌_경우_예외가_발생한다() throws Exception {
            // given
            willThrow(new CustomException(AlbumErrorCode.PARTICIPANT_NOT_IN_ALBUM))
                    .given(participantService)
                    .kickParticipant(1L, 1L);

            // when & then
            ResultActions perform = mockMvc.perform(delete("/albums/1/participants/1"));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("PARTICIPANT_NOT_IN_ALBUM"))
                    .andExpect(jsonPath("$.data.message").value("해당 참가자는 이 앨범에 속해 있지 않습니다."));
        }
    }

    @Nested
    class 참가자_목록_조회_요청_시 {

        @Test
        void 마지막_페이지인_경우_isLast를_true로_응답한다() throws Exception {
            // given
            List<ParticipantListResponse> participants =
                    List.of(
                            new ParticipantListResponse(
                                    2L,
                                    "testNickname2",
                                    "testProfileImageUrl1",
                                    ParticipantRole.STANDARD),
                            new ParticipantListResponse(
                                    1L,
                                    "testNickname1",
                                    "testProfileImageUrl2",
                                    ParticipantRole.HOST));

            given(participantService.getParticipants(1L, null, null, 2))
                    .willReturn(new SliceResponse<>(participants, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(get("/albums/1/participants").param("size", "2"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].participantId").value(2))
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        @Test
        void 마지막_페이지가_아닌_경우_isLast를_false로_응답한다() throws Exception {
            // given
            List<ParticipantListResponse> participants =
                    List.of(
                            new ParticipantListResponse(
                                    2L,
                                    "testNickname2",
                                    "testProfileImageUrl1",
                                    ParticipantRole.STANDARD),
                            new ParticipantListResponse(
                                    1L,
                                    "testNickname1",
                                    "testProfileImageUrl2",
                                    ParticipantRole.HOST));

            given(participantService.getParticipants(1L, null, null, 1))
                    .willReturn(new SliceResponse<>(participants, false));

            // when & then
            ResultActions perform =
                    mockMvc.perform(get("/albums/1/participants").param("size", "1"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].participantId").value(2))
                    .andExpect(jsonPath("$.data.isLast").value(false));
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() throws Exception {
            // given
            given(participantService.getParticipants(1L, null, null, 2))
                    .willThrow(new CustomException(AlbumErrorCode.ALBUM_NOT_FOUND));

            // when & then
            ResultActions perform =
                    mockMvc.perform(get("/albums/1/participants").param("size", "2"));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("ALBUM_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("앨범이 존재하지 않습니다."));
        }

        @Test
        void 앨범_참가자가_아닌_경우_예외가_발생한다() throws Exception {
            // given
            given(participantService.getParticipants(1L, null, null, 2))
                    .willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT));

            // when & then
            ResultActions perform =
                    mockMvc.perform(get("/albums/1/participants").param("size", "2"));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_ALBUM_PARTICIPANT"))
                    .andExpect(jsonPath("$.data.message").value("앨범에 속하지 않은 사용자입니다."));
        }

        @Test
        void lastNickname만_포함된_요청의_경우_예외가_발생한다() throws Exception {
            // given
            given(participantService.getParticipants(1L, "가가가", null, 2))
                    .willThrow(new CustomException(ParticipantErrorCode.MISSING_CURSOR_PAIR));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/albums/1/participants")
                                    .param("size", "2")
                                    .param("lastNickname", "가가가"));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MISSING_CURSOR_PAIR"))
                    .andExpect(
                            jsonPath("$.data.message")
                                    .value(
                                            "lastNickname과 lastParticipantId는 요청에 함께 포함되어야 합니다. 하나만 포함할 수는 없습니다."));
        }

        @Test
        void lastParticipantId만_포함된_요청의_경우_예외가_발생한다() throws Exception {
            // given
            given(participantService.getParticipants(1L, null, 1L, 2))
                    .willThrow(new CustomException(ParticipantErrorCode.MISSING_CURSOR_PAIR));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/albums/1/participants")
                                    .param("size", "2")
                                    .param("lastParticipantId", "1"));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MISSING_CURSOR_PAIR"))
                    .andExpect(
                            jsonPath("$.data.message")
                                    .value(
                                            "lastNickname과 lastParticipantId는 요청에 함께 포함되어야 합니다. 하나만 포함할 수는 없습니다."));
        }

        @ParameterizedTest
        @ValueSource(strings = {"-1", "-999", "0"})
        void 페이지_크기가_0_이하이면_예외가_발생한다(String pageSize) throws Exception {
            // when & then
            ResultActions perform =
                    mockMvc.perform(get("/albums/1/participants").param("size", pageSize));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("ConstraintViolationException"))
                    .andExpect(jsonPath("$.data.message").value("페이지 크기는 0보다 큰 값만 가능합니다."));
        }
    }
}
