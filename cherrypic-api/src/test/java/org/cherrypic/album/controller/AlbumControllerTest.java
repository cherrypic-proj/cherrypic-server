package org.cherrypic.album.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cherrypic.domain.album.controller.AlbumController;
import org.cherrypic.domain.album.dto.request.AlbumCreateRequest;
import org.cherrypic.domain.album.dto.request.InvitationLinkCreateRequest;
import org.cherrypic.domain.album.dto.response.AlbumCreateResponse;
import org.cherrypic.domain.album.dto.response.InvitationLinkCreateResponse;
import org.cherrypic.domain.album.service.AlbumService;
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

@WebMvcTest(AlbumController.class)
@AutoConfigureMockMvc(addFilters = false)
class AlbumControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AlbumService albumService;

    @Nested
    class 앨범_생성_요청_시 {

        @Test
        void 유효한_요청이면_앨범_생성_정보를_반환한다() throws Exception {
            // given
            AlbumCreateRequest request = new AlbumCreateRequest("testTitle", "testCoverUrl");

            AlbumCreateResponse response = new AlbumCreateResponse(1L, "testTitle", "testCoverUrl");

            given(albumService.createAlbum(request)).willReturn(response);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.CREATED.value()))
                    .andExpect(jsonPath("$.data.albumId").value(1))
                    .andExpect(jsonPath("$.data.title").value("testTitle"))
                    .andExpect(jsonPath("$.data.coverUrl").value("testCoverUrl"));
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        @ValueSource(strings = {" "})
        void 앨범_이름이_null_또는_공백이면_예외가_발생한다(String title) throws Exception {
            // given
            AlbumCreateRequest request = new AlbumCreateRequest(title, "testCoverUrl");

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("앨범 이름은 비워둘 수 없습니다."));
        }
    }

    @Nested
    class 앨범_초대_코드_생성_요청_시 {

        @Test
        void 유효한_요청이면_초대_코드_정보를_반환한다() throws Exception {
            // given
            InvitationLinkCreateRequest request = new InvitationLinkCreateRequest(1L);

            InvitationLinkCreateResponse response =
                    new InvitationLinkCreateResponse(
                            "https://dev-api.cherrypic.today/participants/join?code=3FA7A9");

            given(albumService.createInvitationLink(request)).willReturn(response);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/invite-link")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.CREATED.value()))
                    .andExpect(
                            jsonPath("$.data.invitationLink")
                                    .value(
                                            "https://dev-api.cherrypic.today/participants/join?code=3FA7A9"));
        }

        @Test
        void 앨범_id_가_null_이면_예외가_발생한다() throws Exception {
            // given
            InvitationLinkCreateRequest request = new InvitationLinkCreateRequest(null);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/invite-link")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("앨범 ID는 비워둘 수 없습니다."));
        }
    }
}
