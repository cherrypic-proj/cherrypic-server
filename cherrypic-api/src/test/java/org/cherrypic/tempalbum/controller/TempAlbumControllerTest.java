package org.cherrypic.tempalbum.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.cherrypic.domain.tempalbum.controller.TempAlbumController;
import org.cherrypic.domain.tempalbum.dto.request.TempAlbumCreateRequest;
import org.cherrypic.domain.tempalbum.dto.request.TempAlbumUpdateRequest;
import org.cherrypic.domain.tempalbum.dto.response.TempAlbumCreateResponse;
import org.cherrypic.domain.tempalbum.dto.response.TempAlbumInfoResponse;
import org.cherrypic.domain.tempalbum.dto.response.TempAlbumListResponse;
import org.cherrypic.domain.tempalbum.exception.TempAlbumErrorCode;
import org.cherrypic.domain.tempalbum.service.TempAlbumService;
import org.cherrypic.exception.CustomException;
import org.cherrypic.tempalbum.enums.TempAlbumType;
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

@WebMvcTest(TempAlbumController.class)
@AutoConfigureMockMvc(addFilters = false)
class TempAlbumControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private TempAlbumService tempAlbumService;

    @Nested
    class 임시_앨범_생성_요청_시 {

        @Test
        void 유효한_요청이면_임시_앨범을_생성하고_관련_정보를_반환한다() throws Exception {
            // given
            TempAlbumCreateRequest request = new TempAlbumCreateRequest("testTitle");

            TempAlbumCreateResponse response =
                    new TempAlbumCreateResponse(
                            1L,
                            1L,
                            "testTitle",
                            new BigDecimal(0.00),
                            TempAlbumType.DEFAULT,
                            LocalDate.of(2025, 1, 1));

            given(tempAlbumService.createTempAlbum(request)).willReturn(response);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/temp-albums")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.CREATED.value()))
                    .andExpect(jsonPath("$.data.tempAlbumId").value(1))
                    .andExpect(jsonPath("$.data.memberId").value(1))
                    .andExpect(jsonPath("$.data.title").value("testTitle"))
                    .andExpect(jsonPath("$.data.capacityGb").value(0.00))
                    .andExpect(jsonPath("$.data.type").value("DEFAULT"))
                    .andExpect(jsonPath("$.data.expiredAt").value("2025-01-01"));
        }

        @Test
        void 임시_앨범_생성_제한을_넘기면_예외가_발생한다() throws Exception {
            // given
            TempAlbumCreateRequest request = new TempAlbumCreateRequest("testTitle");

            given(tempAlbumService.createTempAlbum(request))
                    .willThrow(new CustomException(TempAlbumErrorCode.CREATE_OVER_LIMIT));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/temp-albums")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("CREATE_OVER_LIMIT"))
                    .andExpect(jsonPath("$.data.message").value("임시 앨범은 5개 이상으로 만들 수 없습니다."));
        }

        @Test
        void 임시_앨범_이름이_20자를_초과하면_예외가_발생한다() throws Exception {
            // given
            TempAlbumCreateRequest request = new TempAlbumCreateRequest("t".repeat(21));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/temp-albums")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("임시 앨범 이름은 최대 20자까지 가능합니다."));
        }
    }

    @Nested
    class 임시_앨범_목록_조회_요청_시 {

        @Test
        void 유효한_요청이면_임시_목록을_반환한다() throws Exception {
            // given
            TempAlbumListResponse response =
                    new TempAlbumListResponse(
                            List.of(
                                    new TempAlbumListResponse.Content(1L, "testTitle1"),
                                    new TempAlbumListResponse.Content(2L, "testTitle1"),
                                    new TempAlbumListResponse.Content(3L, "testTitle1")));

            given(tempAlbumService.getTempAlbums()).willReturn(response);

            // when & then
            ResultActions perform =
                    mockMvc.perform(get("/temp-albums").contentType(MediaType.APPLICATION_JSON));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].tempAlbumId").value(1))
                    .andExpect(jsonPath("$.data.content[1].tempAlbumId").value(2))
                    .andExpect(jsonPath("$.data.content[2].tempAlbumId").value(3));
        }
    }

    @Nested
    class 임시_앨범_수정_요청_시 {

        @Test
        void 유효한_요청이면_임시_앨범을_수정한다() throws Exception {
            // given
            TempAlbumUpdateRequest request = new TempAlbumUpdateRequest("testTitle", "testWebUrl");

            willDoNothing().given(tempAlbumService).updateTempAlbum(1L, request);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/temp-albums/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isNoContent())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NO_CONTENT.value()));
        }

        @Test
        void 수정_도중_임시_앨범이_존재하지_않는_경우_예외가_발생한다() throws Exception {
            // given
            TempAlbumUpdateRequest request = new TempAlbumUpdateRequest("testTitle", "testWebUrl");

            willThrow(new CustomException(TempAlbumErrorCode.TEMP_ALBUM_NOT_FOUND))
                    .given(tempAlbumService)
                    .updateTempAlbum(999L, request);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/temp-albums/999")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("TEMP_ALBUM_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("임시 앨범이 존재하지 않습니다."));
        }

        @Test
        void 수정_도중_임시_앨범_생성자가_아닌_경우_예외가_발생한다() throws Exception {
            // given
            TempAlbumUpdateRequest request = new TempAlbumUpdateRequest("testTitle", "testWebUrl");

            willThrow(new CustomException(TempAlbumErrorCode.NOT_TEMP_ALBUM_OWNER))
                    .given(tempAlbumService)
                    .updateTempAlbum(2L, request);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/temp-albums/2")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_TEMP_ALBUM_OWNER"))
                    .andExpect(jsonPath("$.data.message").value("임시 앨범 소유자가 아닌 경우 권한이 없습니다."));
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        @ValueSource(strings = {" "})
        void 임시_앨범_이름이_null_또는_공백이면_예외가_발생한다(String name) throws Exception {
            // given
            TempAlbumUpdateRequest request = new TempAlbumUpdateRequest(name, "testUrl");

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/temp-albums/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("임시 앨범 이름은 비워둘 수 없습니다."));
        }

        @Nested
        class 임시_앨범_개별_조회_요청_시 {

            @Test
            void 유효한_요청이면_임시_앨범_정보를_반환한다() throws Exception {
                // given
                TempAlbumInfoResponse response =
                        new TempAlbumInfoResponse(
                                "testTitle",
                                new BigDecimal(0.50),
                                new BigDecimal(1.00),
                                LocalDate.of(2025, 1, 1),
                                "testWebUrl");

                given(tempAlbumService.getTempAlbum(1L)).willReturn(response);

                // when & then
                ResultActions perform =
                        mockMvc.perform(
                                get("/temp-albums/1").contentType(MediaType.APPLICATION_JSON));

                perform.andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                        .andExpect(jsonPath("$.data.title").value("testTitle"))
                        .andExpect(jsonPath("$.data.capacityUsedGb").value(0.50))
                        .andExpect(jsonPath("$.data.totalCapacityGb").value(1.00))
                        .andExpect(jsonPath("$.data.expiredDate").value("2025-01-01"))
                        .andExpect(jsonPath("$.data.webUrl").value("testWebUrl"));
            }
        }

        @Test
        void 개별_조회_도중_임시_앨범이_존재하지_않는_경우_예외가_발생한다() throws Exception {
            // given
            given(tempAlbumService.getTempAlbum(1L))
                    .willThrow(new CustomException(TempAlbumErrorCode.TEMP_ALBUM_NOT_FOUND));

            // when & then
            ResultActions perform =
                    mockMvc.perform(get("/temp-albums/1").contentType(MediaType.APPLICATION_JSON));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("TEMP_ALBUM_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("임시 앨범이 존재하지 않습니다."));
        }

        @Test
        void 개별_조회_도중_임시_앨범_생성자가_아닌_경우_예외가_발생한다() throws Exception {
            // given
            given(tempAlbumService.getTempAlbum(1L))
                    .willThrow(new CustomException(TempAlbumErrorCode.NOT_TEMP_ALBUM_OWNER));

            // when & then
            ResultActions perform =
                    mockMvc.perform(get("/temp-albums/1").contentType(MediaType.APPLICATION_JSON));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_TEMP_ALBUM_OWNER"))
                    .andExpect(jsonPath("$.data.message").value("임시 앨범 소유자가 아닌 경우 권한이 없습니다."));
        }
    }
}
