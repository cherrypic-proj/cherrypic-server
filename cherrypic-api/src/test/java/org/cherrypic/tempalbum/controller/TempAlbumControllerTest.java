package org.cherrypic.tempalbum.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.tempalbum.controller.TempAlbumController;
import org.cherrypic.domain.tempalbum.dto.TempAlbumCreateRequest;
import org.cherrypic.domain.tempalbum.service.TempAlbumService;
import org.cherrypic.exception.CustomException;
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

@WebMvcTest(TempAlbumController.class)
@AutoConfigureMockMvc(addFilters = false)
class TempAlbumControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private TempAlbumService tempAlbumService;

    @Nested
    class 임시_앨범을_생성_요청_시 {

        @Test
        void 유효한_요청이면_임시_앨범을_생성하고_임시_앨범에_이미지를_저장하며_NO_CONTENT를_반환한다() throws Exception {
            // given
            TempAlbumCreateRequest request = new TempAlbumCreateRequest(List.of(1L, 2L));

            willDoNothing().given(tempAlbumService).createTempAlbum(1L, request);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/album/1/temp-album")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isNoContent())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NO_CONTENT.value()));
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() throws Exception {
            // given
            TempAlbumCreateRequest request = new TempAlbumCreateRequest(List.of(1L, 2L));

            willThrow(new CustomException(AlbumErrorCode.ALBUM_NOT_FOUND))
                    .given(tempAlbumService)
                    .createTempAlbum(1L, request);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/album/1/temp-album")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("ALBUM_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("앨범이 존재하지 않습니다."));
        }

        @Test
        void 앨범에_속하지_않은_사용자가_임시_앨범을_생성하면_예외가_발생한다() throws Exception {
            // given
            TempAlbumCreateRequest request = new TempAlbumCreateRequest(List.of(1L, 2L));

            willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT))
                    .given(tempAlbumService)
                    .createTempAlbum(1L, request);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/album/1/temp-album")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_ALBUM_PARTICIPANT"))
                    .andExpect(jsonPath("$.data.message").value("앨범에 속하지 않은 사용자입니다."));
        }

        @Test
        void LIMITED_권한의_사용자가_임시_앨범을_생성하면_예외가_발생한다() throws Exception {
            // given
            TempAlbumCreateRequest request = new TempAlbumCreateRequest(List.of(1L, 2L));

            willThrow(new CustomException(AlbumErrorCode.LIMITED_AUTHORITY))
                    .given(tempAlbumService)
                    .createTempAlbum(1L, request);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/album/1/temp-album")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("LIMITED_AUTHORITY"))
                    .andExpect(jsonPath("$.data.message").value("앨범에 대한 생성/수정 권한이 없습니다."));
        }

        @Test
        void 앨범에_속하지_않은_이미지를_추가하면_예외가_발생한다() throws Exception {
            // given
            TempAlbumCreateRequest request = new TempAlbumCreateRequest(List.of(1L, 2L));

            willThrow(new CustomException(AlbumErrorCode.IMAGES_NOT_IN_ALBUM))
                    .given(tempAlbumService)
                    .createTempAlbum(1L, request);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/album/1/temp-album")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("IMAGES_NOT_IN_ALBUM"))
                    .andExpect(jsonPath("$.data.message").value("앨범에 속해 있지 않은 이미지가 포함되어 있습니다."));
        }

        @Test
        void 임시_앨범으로_공유할_이미지_ID들을_비워두면_예외가_발생한다() throws Exception {
            // given
            TempAlbumCreateRequest request = new TempAlbumCreateRequest(List.of());

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/album/1/temp-album")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(
                            jsonPath("$.data.message").value("임시 앨범으로 공유할 이미지 ID들은 비워둘 수 없습니다."));
        }
    }
}
