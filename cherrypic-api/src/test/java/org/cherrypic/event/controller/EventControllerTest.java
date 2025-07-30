package org.cherrypic.event.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.event.controller.EventController;
import org.cherrypic.domain.event.dto.EventCreateRequest;
import org.cherrypic.domain.event.dto.EventCreateResponse;
import org.cherrypic.domain.event.dto.EventUpdateRequest;
import org.cherrypic.domain.event.dto.EventUpdateResponse;
import org.cherrypic.domain.event.exception.EventErrorCode;
import org.cherrypic.domain.event.exception.EventException;
import org.cherrypic.domain.event.service.EventService;
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

@WebMvcTest(EventController.class)
@AutoConfigureMockMvc(addFilters = false)
public class EventControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private EventService eventService;

    @Nested
    class 이벤트_생성_요청_시 {

        @Test
        void 유효한_요청이면_이벤트_생성_정보를_반환한다() throws Exception {
            // given
            EventCreateRequest request = new EventCreateRequest(1L, "testTitle", "testCoverUrl");
            EventCreateResponse response = new EventCreateResponse(1L, "testTitle", "testCoverUrl");

            given(eventService.createEvent(request)).willReturn(response);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/events")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.CREATED.value()))
                    .andExpect(jsonPath("$.data.eventId").value(1))
                    .andExpect(jsonPath("$.data.title").value("testTitle"))
                    .andExpect(jsonPath("$.data.coverUrl").value("testCoverUrl"));
        }

        @ParameterizedTest
        @NullSource
        void 앨범_ID가_null이면_예외가_발생한다(Long albumId) throws Exception {
            // given
            EventCreateRequest request =
                    new EventCreateRequest(albumId, "testTitle", "testCoverUrl");

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/events")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("앨범 ID는 비워둘 수 없습니다."));
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        @ValueSource(strings = {" "})
        void 이벤트_이름이_null_또는_공백이면_예외가_발생한다(String title) throws Exception {
            // given
            EventCreateRequest request = new EventCreateRequest(1L, title, "testCoverUrl");

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/events")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("이벤트 이름은 비워둘 수 없습니다."));
        }

        @Test
        void 이벤트_이름이_20자를_넘어가면_예외가_발생한다() throws Exception {
            // given
            EventCreateRequest request = new EventCreateRequest(1L, "t".repeat(21), "testCoverUrl");

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/events")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("이벤트 이름은 최대 20자까지 가능합니다."));
        }

        @Test
        void 요청자가_앨범_참가자가_아닌_경우_예외가_발생한다() throws Exception {
            // given
            EventCreateRequest request = new EventCreateRequest(1L, "testTitle", "testCoverUrl");

            given(eventService.createEvent(request))
                    .willThrow(new EventException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/events")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_ALBUM_PARTICIPANT"))
                    .andExpect(jsonPath("$.data.message").value("앨범에 속하지 않은 사용자입니다."));
        }

        @Test
        void 요청자가_LIMITED_권한을_가지는_경우_예외가_발생한다() throws Exception {
            // given
            EventCreateRequest request = new EventCreateRequest(1L, "testTitle", "testCoverUrl");

            given(eventService.createEvent(request))
                    .willThrow(new EventException(AlbumErrorCode.LIMITED_AUTHORITY));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/events")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("LIMITED_AUTHORITY"))
                    .andExpect(jsonPath("$.data.message").value("앨범에 대한 생성/수정 권한이 없습니다."));
        }
    }

    @Nested
    class 이벤트_수정_요청_시 {

        @Test
        void 유효한_요청이면_이벤트_수정_정보를_반환한다() throws Exception {
            // given
            EventUpdateRequest request =
                    new EventUpdateRequest("testUpdatedTitle", "testUpdatedCoverUrl");
            EventUpdateResponse response =
                    new EventUpdateResponse(1L, "testUpdatedTitle", "testUpdatedCoverUrl");

            given(eventService.updateEvent(1L, request)).willReturn(response);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/events/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.eventId").value(1))
                    .andExpect(jsonPath("$.data.title").value("testUpdatedTitle"))
                    .andExpect(jsonPath("$.data.coverUrl").value("testUpdatedCoverUrl"));
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        @ValueSource(strings = {" "})
        void 이벤트_이름이_null_또는_공백이면_예외가_발생한다(String title) throws Exception {
            // given
            EventUpdateRequest request = new EventUpdateRequest(title, "testUpdatedCoverUrl");

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/events/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("이벤트 이름은 비워둘 수 없습니다."));
        }

        @Test
        void 이벤트_이름이_20자를_넘어가면_예외가_발생한다() throws Exception {
            // given
            EventUpdateRequest request =
                    new EventUpdateRequest("t".repeat(21), "testUpdatedCoverUrl");

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/events/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("이벤트 이름은 최대 20자까지 가능합니다."));
        }

        @Test
        void 요청자가_앨범_참가자가_아닌_경우_예외가_발생한다() throws Exception {
            // given
            EventCreateRequest request = new EventCreateRequest(1L, "testTitle", "testCoverUrl");

            given(eventService.createEvent(request))
                    .willThrow(new EventException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/events")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_ALBUM_PARTICIPANT"))
                    .andExpect(jsonPath("$.data.message").value("앨범에 속하지 않은 사용자입니다."));
        }

        @Test
        void 요청자가_LIMITED_권한을_가지는_경우_예외가_발생한다() throws Exception {
            // given
            EventCreateRequest request = new EventCreateRequest(1L, "testTitle", "testCoverUrl");

            given(eventService.createEvent(request))
                    .willThrow(new EventException(AlbumErrorCode.LIMITED_AUTHORITY));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/events")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("LIMITED_AUTHORITY"))
                    .andExpect(jsonPath("$.data.message").value("앨범에 대한 생성/수정 권한이 없습니다."));
        }

        @Nested
        class 이벤트_삭제_요청_시 {

            @Test
            void 유효한_요청이면_이벤트를_삭제하고_NO_CONTENT_로_반환한다() throws Exception {
                // given
                willDoNothing().given(eventService).deleteEvent(1L);

                // when & then
                ResultActions perform =
                        mockMvc.perform(
                                delete("/events/1").contentType(MediaType.APPLICATION_JSON));

                perform.andExpect(status().isNoContent())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.status").value(HttpStatus.NO_CONTENT.value()));
            }

            @Test
            void 이벤트가_존재하지_않는_경우_예외가_발생한다() throws Exception {
                // given
                willThrow(new EventException(EventErrorCode.EVENT_NOT_FOUND))
                        .given(eventService)
                        .deleteEvent(1L);

                // when & then
                ResultActions perform =
                        mockMvc.perform(
                                delete("/events/1").contentType(MediaType.APPLICATION_JSON));

                perform.andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                        .andExpect(jsonPath("$.data.code").value("EVENT_NOT_FOUND"))
                        .andExpect(jsonPath("$.data.message").value("존재하지 않는 이벤트입니다."));
            }
        }
    }
}
