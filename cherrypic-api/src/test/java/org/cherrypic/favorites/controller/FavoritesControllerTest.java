package org.cherrypic.favorites.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.favorites.controller.FavoritesController;
import org.cherrypic.domain.favorites.dto.response.FavoritesMarkToggleResponse;
import org.cherrypic.domain.favorites.service.FavoritesService;
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

@WebMvcTest(FavoritesController.class)
@AutoConfigureMockMvc(addFilters = false)
class FavoritesControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private FavoritesService favoritesService;

    @Nested
    class 앨범_즐겨찾기_토글_상태_변경_요청_시 {

        @Test
        void 유효한_요청이면_앨범의_즐겨찾기_상태가_변경된다() throws Exception {
            // given
            FavoritesMarkToggleResponse response = new FavoritesMarkToggleResponse(true);

            given(favoritesService.toggleMark(1L)).willReturn(response);

            // when & then
            ResultActions perform = mockMvc.perform(patch("/albums/1/favorites"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.marked").value(true));
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() throws Exception {
            // given
            given(favoritesService.toggleMark(1L))
                    .willThrow(new CustomException(AlbumErrorCode.ALBUM_NOT_FOUND));

            // when & then
            ResultActions perform = mockMvc.perform(patch("/albums/1/favorites"));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("ALBUM_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("앨범이 존재하지 않습니다."));
        }

        @Test
        void 앨범_참가자가_아닌_경우_예외가_발생한다() throws Exception {
            // given
            given(favoritesService.toggleMark(1L))
                    .willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT));

            // when & then
            ResultActions perform = mockMvc.perform(patch("/albums/1/favorites"));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_ALBUM_PARTICIPANT"))
                    .andExpect(jsonPath("$.data.message").value("앨범에 속하지 않은 사용자입니다."));
        }
    }
}
