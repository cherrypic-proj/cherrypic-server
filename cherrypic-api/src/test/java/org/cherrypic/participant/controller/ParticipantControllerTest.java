package org.cherrypic.participant.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cherrypic.domain.album.dto.response.*;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.participant.controller.ParticipantController;
import org.cherrypic.domain.participant.exception.ParticipantErrorCode;
import org.cherrypic.domain.participant.service.ParticipantService;
import org.cherrypic.exception.CustomException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
        void 강퇴_요청자와_강퇴_대상이_동일한_경우_예외가_발생한다() throws Exception {
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
}
