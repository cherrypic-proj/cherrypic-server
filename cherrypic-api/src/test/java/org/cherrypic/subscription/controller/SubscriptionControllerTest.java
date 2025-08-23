package org.cherrypic.subscription.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cherrypic.domain.album.dto.response.*;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.subscription.controller.SubscriptionController;
import org.cherrypic.domain.subscription.exception.SubscriptionErrorCode;
import org.cherrypic.domain.subscription.service.SubscriptionService;
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

@WebMvcTest(SubscriptionController.class)
@AutoConfigureMockMvc(addFilters = false)
class SubscriptionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private SubscriptionService subscriptionService;

    @Nested
    class 구독_해지_요청_시 {

        @Test
        void 유효한_요청이면_구독을_해지하고_NO_CONTENT_로_반환한다() throws Exception {
            // given
            willDoNothing().given(subscriptionService).cancelSubscription(1L);

            // when & then
            ResultActions perform = mockMvc.perform(delete("/albums/1/subscriptions"));

            perform.andExpect(status().isNoContent())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NO_CONTENT.value()));
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() throws Exception {
            // given
            willThrow(new CustomException(AlbumErrorCode.ALBUM_NOT_FOUND))
                    .given(subscriptionService)
                    .cancelSubscription(1L);

            // when & then
            ResultActions perform = mockMvc.perform(delete("/albums/1/subscriptions"));

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
                    .given(subscriptionService)
                    .cancelSubscription(1L);

            // when & then
            ResultActions perform = mockMvc.perform(delete("/albums/1/subscriptions"));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_ALBUM_PARTICIPANT"))
                    .andExpect(jsonPath("$.data.message").value("앨범에 속하지 않은 사용자입니다."));
        }

        @Test
        void 앨범_방장이_아닌_경우_예외가_발생한다() throws Exception {
            // given
            willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_HOST))
                    .given(subscriptionService)
                    .cancelSubscription(1L);

            // when & then
            ResultActions perform = mockMvc.perform(delete("/albums/1/subscriptions"));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_ALBUM_HOST"))
                    .andExpect(jsonPath("$.data.message").value("방장이 아닌 경우 권한이 없습니다."));
        }

        @Test
        void BASIC_플랜인_경우_예외가_발생한다() throws Exception {
            // given
            willThrow(
                            new CustomException(
                                    SubscriptionErrorCode
                                            .SUBSCRIPTION_NOT_SUPPORTED_FOR_BASIC_PLAN))
                    .given(subscriptionService)
                    .cancelSubscription(1L);

            // when & then
            ResultActions perform = mockMvc.perform(delete("/albums/1/subscriptions"));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(
                            jsonPath("$.data.code")
                                    .value("SUBSCRIPTION_NOT_SUPPORTED_FOR_BASIC_PLAN"))
                    .andExpect(jsonPath("$.data.message").value("BASIC 플랜은 구독 기능을 지원하지 않습니다."));
        }

        @Test
        void 구독이_존재하지_않는_경우_예외가_발생한다() throws Exception {
            // given
            willThrow(new CustomException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND))
                    .given(subscriptionService)
                    .cancelSubscription(1L);

            // when & then
            ResultActions perform = mockMvc.perform(delete("/albums/1/subscriptions"));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("SUBSCRIPTION_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("구독 정보가 존재하지 않습니다."));
        }

        @Test
        void 이미_해지된_구독이면_예외가_발생한다() throws Exception {
            // given
            willThrow(new CustomException(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_CANCELED))
                    .given(subscriptionService)
                    .cancelSubscription(1L);

            // when & then
            ResultActions perform = mockMvc.perform(delete("/albums/1/subscriptions"));

            perform.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                    .andExpect(jsonPath("$.data.code").value("SUBSCRIPTION_ALREADY_CANCELED"))
                    .andExpect(jsonPath("$.data.message").value("이미 해지된 구독입니다."));
        }

        @Test
        void 종료된_구독이면_예외가_발생한다() throws Exception {
            // given
            willThrow(new CustomException(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_ENDED))
                    .given(subscriptionService)
                    .cancelSubscription(1L);

            // when & then
            ResultActions perform = mockMvc.perform(delete("/albums/1/subscriptions"));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("SUBSCRIPTION_ALREADY_ENDED"))
                    .andExpect(jsonPath("$.data.message").value("이미 종료된 구독입니다. 해지 또는 갱신할 수 없습니다."));
        }
    }
}
