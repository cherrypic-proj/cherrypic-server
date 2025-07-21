package org.cherrypic.event.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cherrypic.domain.event.controller.EventController;
import org.cherrypic.domain.event.dto.EventCreateRequest;
import org.cherrypic.domain.event.dto.EventCreateResponse;
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
    class 이벤트를_만들_때 {

        @Test
        void 유효한_요청이면_이벤트_생성_정보를_반환한다() throws Exception {
            // given
            EventCreateRequest request = new EventCreateRequest(1L, "Test Event", "Test CoverURL");
            EventCreateResponse response =
                    new EventCreateResponse(1L, 1L, "Test Event", "Test CoverURL");

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
                    .andExpect(jsonPath("$.data.albumId").value(1))
                    .andExpect(jsonPath("$.data.eventId").value(1))
                    .andExpect(jsonPath("$.data.eventTitle").value("Test Event"))
                    .andExpect(jsonPath("$.data.coverUrl").value("Test CoverURL"));
        }

        @NullSource
        @ParameterizedTest
        void 엘범_ID가_null이면_에러가_발생한다(Long albumId) throws Exception {
            // given
            EventCreateRequest request =
                    new EventCreateRequest(albumId, "Test Event", "Test CoverURL");

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
                    .andExpect(jsonPath("$.data.message").value("엘범 ID는 비워둘 수 없습니다."));
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        @ValueSource(strings = {" "})
        void 이벤트_이름이_null_또는_공백이면_예외가_발생한다(String title) throws Exception {
            // given
            EventCreateRequest request = new EventCreateRequest(1L, title, "Test CoverURL");

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
        void 이벤트_이름이_100자를_넘어가면_에러가_발생한다() throws Exception {
            // given
            EventCreateRequest request =
                    new EventCreateRequest(1L, "a".repeat(101), "Test CoverURL");

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
                    .andExpect(jsonPath("$.data.message").value("이벤트 이름은 최대 100자까지 가능합니다."));
        }
    }
}
