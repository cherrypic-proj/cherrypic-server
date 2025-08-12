package org.cherrypic.event.controller;

import static org.mockito.BDDMockito.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.event.controller.EventController;
import org.cherrypic.domain.event.dto.request.EventCreateRequest;
import org.cherrypic.domain.event.dto.request.EventImageAddRequest;
import org.cherrypic.domain.event.dto.request.EventUpdateRequest;
import org.cherrypic.domain.event.dto.response.EventCreateResponse;
import org.cherrypic.domain.event.dto.response.EventListResponse;
import org.cherrypic.domain.event.dto.response.EventUpdateResponse;
import org.cherrypic.domain.event.exception.EventErrorCode;
import org.cherrypic.domain.event.exception.EventException;
import org.cherrypic.domain.event.service.EventService;
import org.cherrypic.domain.image.exception.ImageErrorCode;
import org.cherrypic.exception.BaseCustomException;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;
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
        void 이벤트가_존재하지_않는_경우_예외가_발생한다() throws Exception {
            // given
            EventUpdateRequest request =
                    new EventUpdateRequest("testUpdatedTitle", "testUpdatedCoverUrl");

            given(eventService.updateEvent(1L, request))
                    .willThrow(new EventException(EventErrorCode.EVENT_NOT_FOUND));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/events/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("EVENT_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("존재하지 않는 이벤트입니다."));
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

            @Test
            void 요청자가_앨범_참가자가_아닌_경우_예외가_발생한다() throws Exception {
                // given
                willThrow(new EventException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT))
                        .given(eventService)
                        .deleteEvent(1L);

                // when & then
                ResultActions perform =
                        mockMvc.perform(
                                delete("/events/1").contentType(MediaType.APPLICATION_JSON));

                perform.andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                        .andExpect(jsonPath("$.data.code").value("NOT_ALBUM_PARTICIPANT"))
                        .andExpect(jsonPath("$.data.message").value("앨범에 속하지 않은 사용자입니다."));
            }

            @Test
            void 요청자가_LIMITED_권한을_가지는_경우_예외가_발생한다() throws Exception {
                // given
                willThrow(new EventException(AlbumErrorCode.LIMITED_AUTHORITY))
                        .given(eventService)
                        .deleteEvent(1L);

                // when & then
                ResultActions perform =
                        mockMvc.perform(
                                delete("/events/1").contentType(MediaType.APPLICATION_JSON));

                perform.andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                        .andExpect(jsonPath("$.data.code").value("LIMITED_AUTHORITY"))
                        .andExpect(jsonPath("$.data.message").value("앨범에 대한 생성/수정 권한이 없습니다."));
            }
        }
    }

    @Nested
    class 이벤트_조회_요청시 {

        @Test
        void 정렬_조건이_ASC이면_eventId를_오름차순으로_응답한다() throws Exception {
            // given
            List<EventListResponse> events =
                    List.of(
                            new EventListResponse(1L, "testEventTitle1", "testCoverUrl1", 2),
                            new EventListResponse(2L, "testEventTitle2", "testCoverUrl2", 2));

            given(eventService.getAlbumEvents(1L, null, 2, SortDirection.ASC))
                    .willReturn(new SliceResponse<>(events, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/events")
                                    .param("albumId", "1")
                                    .param("size", "2")
                                    .param("direction", "ASC"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].eventId").value(1))
                    .andExpect(jsonPath("$.data.content[1].eventId").value(2))
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        @Test
        void 정렬_조건이_DESC이면_eventId를_내림차순으로_응답한다() throws Exception {
            // given
            List<EventListResponse> events =
                    List.of(
                            new EventListResponse(2L, "testEventTitle2", "testCoverUrl2", 2),
                            new EventListResponse(1L, "testEventTitle1", "testCoverUrl1", 2));

            given(eventService.getAlbumEvents(1L, null, 2, SortDirection.DESC))
                    .willReturn(new SliceResponse<>(events, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/events")
                                    .param("albumId", "1")
                                    .param("size", "2")
                                    .param("direction", "DESC"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].eventId").value(2))
                    .andExpect(jsonPath("$.data.content[1].eventId").value(1))
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        @Test
        void 마지막_페이지인_경우_isLast를_true로_응답한다() throws Exception {
            // given
            List<EventListResponse> events =
                    List.of(new EventListResponse(1L, "testEventTitle1", "testCoverUrl1", 2));

            given(eventService.getAlbumEvents(1L, null, 1, SortDirection.DESC))
                    .willReturn(new SliceResponse<>(events, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/events")
                                    .param("albumId", "1")
                                    .param("size", "1")
                                    .param("direction", "DESC"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].eventId").value(1))
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        @Test
        void 마지막_페이지가_아닌_경우_isLast를_false로_응답한다() throws Exception {
            // given
            List<EventListResponse> events =
                    List.of(
                            new EventListResponse(2L, "testEventTitle2", "testCoverUrl2", 2),
                            new EventListResponse(1L, "testEventTitle1", "testCoverUrl1", 2));

            given(eventService.getAlbumEvents(1L, null, 1, SortDirection.DESC))
                    .willReturn(new SliceResponse<>(events, false));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/events")
                                    .param("albumId", "1")
                                    .param("size", "1")
                                    .param("direction", "DESC"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].eventId").value(2))
                    .andExpect(jsonPath("$.data.isLast").value(false));
        }

        @Test
        void 이벤트가_없는_경우_빈_리스트를_응답한다() throws Exception {
            // given
            List<EventListResponse> events = List.of();

            given(eventService.getAlbumEvents(1L, null, 10, SortDirection.DESC))
                    .willReturn(new SliceResponse<>(events, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/events")
                                    .param("albumId", "1")
                                    .param("size", "10")
                                    .param("direction", "DESC"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content").isEmpty())
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        @ParameterizedTest
        @ValueSource(strings = {"-1", "-999", "0"})
        void 페이지_크기를_0_이하로_설정하면_예외가_발생한다(String pageSize) throws Exception {
            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/events")
                                    .param("albumId", "1")
                                    .param("size", pageSize)
                                    .param("direction", "DESC"));

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
                            get("/events")
                                    .param("albumId", "1")
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
    class 이벤트_이미지_추가_요청_시 {

        @Test
        void 유효한_요청이면_이벤트에_이미지를_추가하고_NO_CONTENT_로_반환한다() throws Exception {
            // given
            EventImageAddRequest request = new EventImageAddRequest(List.of(1L));
            willDoNothing().given(eventService).addImages(1L, request);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/events/1/add-images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isNoContent())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NO_CONTENT.value()));
        }

        @Test
        void 존재하지_않는_이벤트에_추가하면_예외가_발생한다() throws Exception {
            // given
            EventImageAddRequest request = new EventImageAddRequest(List.of(1L));
            willThrow(new EventException(EventErrorCode.EVENT_NOT_FOUND))
                    .given(eventService)
                    .addImages(1L, request);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/events/1/add-images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("EVENT_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("존재하지 않는 이벤트입니다."));
        }

        @Test
        void 존재하지_않는_이미지를_추가하면_예외가_발생한다() throws Exception {
            // given
            EventImageAddRequest request = new EventImageAddRequest(List.of(999L));
            willThrow(new BaseCustomException(ImageErrorCode.IMAGES_NOT_FOUND))
                    .given(eventService)
                    .addImages(1L, request);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/events/1/add-images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("IMAGES_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("존재하지 않는 이미지를 포함하고 있습니다."));
        }

        @Test
        void LIMITED_권한의_사용자가_이벤트에_이미지를_추가하면_예외가_발생한다() throws Exception {
            // given
            EventImageAddRequest request = new EventImageAddRequest(List.of(1L));
            willThrow(new EventException(AlbumErrorCode.LIMITED_AUTHORITY))
                    .given(eventService)
                    .addImages(1L, request);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/events/1/add-images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("LIMITED_AUTHORITY"))
                    .andExpect(jsonPath("$.data.message").value("앨범에 대한 생성/수정 권한이 없습니다."));
        }

        @Test
        void 이미_이벤트에_속한_이미지를_추가하면_예외가_발생한다() throws Exception {
            // given
            EventImageAddRequest request = new EventImageAddRequest(List.of(1L));
            willThrow(new BaseCustomException(ImageErrorCode.IMAGES_ASSIGNED_TO_EVENT))
                    .given(eventService)
                    .addImages(1L, request);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/events/1/add-images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("IMAGES_ASSIGNED_TO_EVENT"))
                    .andExpect(jsonPath("$.data.message").value("이미 이벤트에 소속된 이미지를 포함하고 있습니다."));
        }

        @Test
        void 다른_앨범에_속한_이미지를_추가하면_예외가_발생한다() throws Exception {
            // given
            EventImageAddRequest request = new EventImageAddRequest(List.of(1L));
            willThrow(new BaseCustomException(ImageErrorCode.IMAGES_FROM_OTHER_ALBUM))
                    .given(eventService)
                    .addImages(1L, request);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/events/1/add-images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("IMAGES_FROM_OTHER_ALBUM"))
                    .andExpect(jsonPath("$.data.message").value("앨범 소속이 아닌 이미지를 포함하고 있습니다."));
        }

        @Test
        void 추가할_이미지를_입력하지_않은_경우_예외가_발생한다() throws Exception {
            // given
            EventImageAddRequest request = new EventImageAddRequest(List.of());

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/events/1/add-images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("추가할 이미지 ID는 비워둘 수 없습니다."));
        }

        @Test
        void 앨범에_이미지를_추가하던_와중_다른_사람이_해당_이미지를_조작하면_예외가_발생한다() throws Exception {
            // given
            EventImageAddRequest request = new EventImageAddRequest(List.of(1L));
            willThrow(new BaseCustomException(ImageErrorCode.CONFLICTING_IMAGES))
                    .given(eventService)
                    .addImages(1L, request);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/events/1/add-images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                    .andExpect(jsonPath("$.data.code").value("CONFLICTING_IMAGES"))
                    .andExpect(jsonPath("$.data.message").value("다른 요청에서 조작된 이미지를 포함하고 있습니다."));
        }
    }
}
