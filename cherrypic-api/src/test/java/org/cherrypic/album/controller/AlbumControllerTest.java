package org.cherrypic.album.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cherrypic.album.enums.AlbumType;
import org.cherrypic.domain.album.controller.AlbumController;
import org.cherrypic.domain.album.dto.request.AlbumCreateRequest;
import org.cherrypic.domain.album.dto.response.AlbumCreateResponse;
import org.cherrypic.domain.album.service.AlbumService;
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

@WebMvcTest(AlbumController.class)
@AutoConfigureMockMvc(addFilters = false)
class AlbumControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AlbumService albumService;

    @Nested
    class 앨범을_생성할_때 {

        @Test
        void 유효한_요청이면_앨범_생성_정보를_반환한다() throws Exception {
            // given
            AlbumCreateRequest request =
                    new AlbumCreateRequest("testTitle", "testCoverUrl", AlbumType.PRIVATE);

            AlbumCreateResponse response =
                    new AlbumCreateResponse(1L, "testTitle", "testCoverUrl", AlbumType.PRIVATE);

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
                    .andExpect(jsonPath("$.data.coverUrl").value("testCoverUrl"))
                    .andExpect(jsonPath("$.data.type").value("PRIVATE"));
        }

        @Test
        void 앨범_이름이_blank이면_예외가_발생한다() throws Exception {
            // given
            AlbumCreateRequest request =
                    new AlbumCreateRequest("", "testCoverUrl", AlbumType.PRIVATE);

            AlbumCreateResponse response =
                    new AlbumCreateResponse(1L, "", "testCoverUrl", AlbumType.PRIVATE);

            given(albumService.createAlbum(request)).willReturn(response);

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

        @Test
        void 앨범_유형이_null이면_예외가_발생한다() throws Exception {
            // given
            AlbumCreateRequest request = new AlbumCreateRequest("testTitle", "testCoverUrl", null);

            AlbumCreateResponse response =
                    new AlbumCreateResponse(1L, "testTitle", "testCoverUrl", null);

            given(albumService.createAlbum(request)).willReturn(response);

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
                    .andExpect(jsonPath("$.data.message").value("앨범 유형은 비워둘 수 없습니다."));
        }
    }
}
