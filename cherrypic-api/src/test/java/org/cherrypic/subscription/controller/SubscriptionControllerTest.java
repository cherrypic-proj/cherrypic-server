package org.cherrypic.subscription.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.payment.exception.PaymentErrorCode;
import org.cherrypic.domain.subscription.controller.SubscriptionController;
import org.cherrypic.domain.subscription.dto.request.SubscriptionRenewRequest;
import org.cherrypic.domain.subscription.dto.response.SubscriptionInfoResponse;
import org.cherrypic.domain.subscription.dto.response.SubscriptionRenewResponse;
import org.cherrypic.domain.subscription.exception.SubscriptionErrorCode;
import org.cherrypic.domain.subscription.service.SubscriptionService;
import org.cherrypic.exception.CustomException;
import org.cherrypic.payment.exception.PaymentDomainErrorCode;
import org.cherrypic.subscription.enums.SubscriptionStatus;
import org.cherrypic.subscription.exception.SubscriptionDomainErrorCode;
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
        void 구독이_만료된_앨범인_경우_예외가_발생한다() throws Exception {
            // given
            willThrow(new CustomException(AlbumErrorCode.EXPIRED_SUBSCRIPTION))
                    .given(subscriptionService)
                    .cancelSubscription(1L);

            // when & then
            ResultActions perform = mockMvc.perform(delete("/albums/1/subscriptions"));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("EXPIRED_SUBSCRIPTION"))
                    .andExpect(jsonPath("$.data.message").value("만료된 앨범에서는 요청을 처리할 수 없습니다."));
        }

        @Test
        void BASIC_유형인_경우_예외가_발생한다() throws Exception {
            // given
            willThrow(
                            new CustomException(
                                    SubscriptionErrorCode
                                            .SUBSCRIPTION_NOT_SUPPORTED_FOR_BASIC_TYPE))
                    .given(subscriptionService)
                    .cancelSubscription(1L);

            // when & then
            ResultActions perform = mockMvc.perform(delete("/albums/1/subscriptions"));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(
                            jsonPath("$.data.code")
                                    .value("SUBSCRIPTION_NOT_SUPPORTED_FOR_BASIC_TYPE"))
                    .andExpect(jsonPath("$.data.message").value("BASIC 유형은 구독 기능을 지원하지 않습니다."));
        }

        @Test
        void 이미_해지된_구독이면_예외가_발생한다() throws Exception {
            // given
            willThrow(new CustomException(SubscriptionDomainErrorCode.ALREADY_CANCELED))
                    .given(subscriptionService)
                    .cancelSubscription(1L);

            // when & then
            ResultActions perform = mockMvc.perform(delete("/albums/1/subscriptions"));

            perform.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                    .andExpect(jsonPath("$.data.code").value("ALREADY_CANCELED"))
                    .andExpect(jsonPath("$.data.message").value("이미 해지된 구독입니다."));
        }

        @Test
        void 만료된_구독이면_예외가_발생한다() throws Exception {
            // given
            willThrow(new CustomException(SubscriptionDomainErrorCode.ALREADY_EXPIRED))
                    .given(subscriptionService)
                    .cancelSubscription(1L);

            // when & then
            ResultActions perform = mockMvc.perform(delete("/albums/1/subscriptions"));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("ALREADY_EXPIRED"))
                    .andExpect(jsonPath("$.data.message").value("이미 만료된 구독입니다. 해지 또는 갱신할 수 없습니다."));
        }
    }

    @Nested
    class 구독_갱신_요청_시 {

        @Test
        void 유효한_요청이면_구독을_갱신하고_갱신_정보를_반환한다() throws Exception {
            // given
            SubscriptionRenewRequest request = new SubscriptionRenewRequest(1L);
            SubscriptionRenewResponse response =
                    new SubscriptionRenewResponse(
                            LocalDateTime.of(2025, 7, 1, 0, 0),
                            LocalDateTime.of(2025, 8, 1, 0, 0),
                            LocalDateTime.of(2025, 8, 2, 0, 0),
                            SubscriptionStatus.ACTIVE);

            given(subscriptionService.renewSubscription(1L, request)).willReturn(response);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/albums/1/subscriptions/renew")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.subscriptionStartAt").value("2025-07-01"))
                    .andExpect(jsonPath("$.data.subscriptionEndAt").value("2025-08-01"))
                    .andExpect(jsonPath("$.data.subscriptionNextBillingAt").value("2025-08-02"))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() throws Exception {
            // given
            SubscriptionRenewRequest request = new SubscriptionRenewRequest(1L);

            given(subscriptionService.renewSubscription(1L, request))
                    .willThrow(new CustomException(AlbumErrorCode.ALBUM_NOT_FOUND));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/albums/1/subscriptions/renew")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("ALBUM_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("앨범이 존재하지 않습니다."));
        }

        @Test
        void 앨범_참가자가_아닌_경우_예외가_발생한다() throws Exception {
            // given
            SubscriptionRenewRequest request = new SubscriptionRenewRequest(1L);

            given(subscriptionService.renewSubscription(1L, request))
                    .willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/albums/1/subscriptions/renew")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_ALBUM_PARTICIPANT"))
                    .andExpect(jsonPath("$.data.message").value("앨범에 속하지 않은 사용자입니다."));
        }

        @Test
        void 앨범_방장이_아닌_경우_예외가_발생한다() throws Exception {
            // given
            SubscriptionRenewRequest request = new SubscriptionRenewRequest(1L);

            given(subscriptionService.renewSubscription(1L, request))
                    .willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_HOST));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/albums/1/subscriptions/renew")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_ALBUM_HOST"))
                    .andExpect(jsonPath("$.data.message").value("방장이 아닌 경우 권한이 없습니다."));
        }

        @Test
        void 구독이_만료된_앨범인_경우_예외가_발생한다() throws Exception {
            // given
            SubscriptionRenewRequest request = new SubscriptionRenewRequest(1L);

            given(subscriptionService.renewSubscription(1L, request))
                    .willThrow(new CustomException(AlbumErrorCode.EXPIRED_SUBSCRIPTION));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/albums/1/subscriptions/renew")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("EXPIRED_SUBSCRIPTION"))
                    .andExpect(jsonPath("$.data.message").value("만료된 앨범에서는 요청을 처리할 수 없습니다."));
        }

        @Test
        void BASIC_유형인_경우_예외가_발생한다() throws Exception {
            // given
            SubscriptionRenewRequest request = new SubscriptionRenewRequest(1L);

            given(subscriptionService.renewSubscription(1L, request))
                    .willThrow(
                            new CustomException(
                                    SubscriptionErrorCode
                                            .SUBSCRIPTION_NOT_SUPPORTED_FOR_BASIC_TYPE));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/albums/1/subscriptions/renew")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(
                            jsonPath("$.data.code")
                                    .value("SUBSCRIPTION_NOT_SUPPORTED_FOR_BASIC_TYPE"))
                    .andExpect(jsonPath("$.data.message").value("BASIC 유형은 구독 기능을 지원하지 않습니다."));
        }

        @Test
        void 결제가_존재하지_않는_경우_예외가_발생한다() throws Exception {
            // given
            SubscriptionRenewRequest request = new SubscriptionRenewRequest(1L);

            given(subscriptionService.renewSubscription(1L, request))
                    .willThrow(new CustomException(PaymentErrorCode.PAYMENT_NOT_FOUND));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/albums/1/subscriptions/renew")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("PAYMENT_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("결제 정보가 존재하지 않습니다."));
        }

        @Test
        void 결제한_회원과_구독을_갱신하려는_회원이_일치하지_않으면_예외가_발생한다() throws Exception {
            // given
            SubscriptionRenewRequest request = new SubscriptionRenewRequest(1L);

            given(subscriptionService.renewSubscription(1L, request))
                    .willThrow(new CustomException(PaymentErrorCode.PAYMENT_MEMBER_MISMATCH));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/albums/1/subscriptions/renew")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("PAYMENT_MEMBER_MISMATCH"))
                    .andExpect(jsonPath("$.data.message").value("결제한 사용자와 일치하지 않습니다."));
        }

        @Test
        void 이미_취소된_결제라면_예외가_발생한다() throws Exception {
            // given
            SubscriptionRenewRequest request = new SubscriptionRenewRequest(1L);

            given(subscriptionService.renewSubscription(1L, request))
                    .willThrow(new CustomException(PaymentDomainErrorCode.ALREADY_CANCELED));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/albums/1/subscriptions/renew")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("ALREADY_CANCELED"))
                    .andExpect(jsonPath("$.data.message").value("해당 결제는 이미 취소되었습니다."));
        }

        @Test
        void 완료되지_않은_결제라면_예외가_발생한다() throws Exception {
            // given
            SubscriptionRenewRequest request = new SubscriptionRenewRequest(1L);

            given(subscriptionService.renewSubscription(1L, request))
                    .willThrow(new CustomException(PaymentDomainErrorCode.NOT_PAID));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/albums/1/subscriptions/renew")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_PAID"))
                    .andExpect(jsonPath("$.data.message").value("아직 결제가 완료되지 않았습니다."));
        }

        @Test
        void 결제가_이미_사용된_경우_예외가_발생한다() throws Exception {
            // given
            SubscriptionRenewRequest request = new SubscriptionRenewRequest(1L);

            given(subscriptionService.renewSubscription(1L, request))
                    .willThrow(new CustomException(PaymentDomainErrorCode.ALREADY_USED_PAYMENT));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/albums/1/subscriptions/renew")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("ALREADY_USED_PAYMENT"))
                    .andExpect(jsonPath("$.data.message").value("해당 결제는 이미 사용되었습니다."));
        }

        @Test
        void 결제의_목적이_구독_갱신과_일치하지_않으면_예외가_발생한다() throws Exception {
            // given
            SubscriptionRenewRequest request = new SubscriptionRenewRequest(1L);

            given(subscriptionService.renewSubscription(1L, request))
                    .willThrow(
                            new CustomException(PaymentDomainErrorCode.PAYMENT_PURPOSE_MISMATCH));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/albums/1/subscriptions/renew")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("PAYMENT_PURPOSE_MISMATCH"))
                    .andExpect(jsonPath("$.data.message").value("결제 목적이 요청하려는 작업과 일치하지 않습니다."));
        }

        @Test
        void 이미_구독_중인_상태면_예외가_발생한다() throws Exception {
            // given
            SubscriptionRenewRequest request = new SubscriptionRenewRequest(1L);

            given(subscriptionService.renewSubscription(1L, request))
                    .willThrow(new CustomException(SubscriptionDomainErrorCode.ALREADY_ACTIVE));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/albums/1/subscriptions/renew")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                    .andExpect(jsonPath("$.data.code").value("ALREADY_ACTIVE"))
                    .andExpect(jsonPath("$.data.message").value("이미 구독 중인 상태입니다."));
        }

        @Test
        void 만료된_구독이면_예외가_발생한다() throws Exception {
            // given
            SubscriptionRenewRequest request = new SubscriptionRenewRequest(1L);

            given(subscriptionService.renewSubscription(1L, request))
                    .willThrow(new CustomException(SubscriptionDomainErrorCode.ALREADY_EXPIRED));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/albums/1/subscriptions/renew")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("ALREADY_EXPIRED"))
                    .andExpect(jsonPath("$.data.message").value("이미 만료된 구독입니다. 해지 또는 갱신할 수 없습니다."));
        }
    }

    @Nested
    class 구독_정보_조회_요청_시 {

        @Test
        void 유효한_요청이면_구독_정보를_반환한다() throws Exception {
            // given
            SubscriptionInfoResponse response =
                    new SubscriptionInfoResponse(
                            LocalDateTime.of(2025, 7, 1, 0, 0),
                            LocalDateTime.of(2025, 8, 1, 0, 0),
                            LocalDateTime.of(2025, 7, 29, 0, 0),
                            SubscriptionStatus.ACTIVE);

            given(subscriptionService.getSubscriptionInfo(1L)).willReturn(response);

            // when & then
            ResultActions perform = mockMvc.perform(get("/albums/1/subscriptions"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.subscriptionStartAt").value("2025-07-01"))
                    .andExpect(jsonPath("$.data.subscriptionEndAt").value("2025-08-01"))
                    .andExpect(jsonPath("$.data.subscriptionNextBillingAt").value("2025-07-29"))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() throws Exception {
            // given
            given(subscriptionService.getSubscriptionInfo(1L))
                    .willThrow(new CustomException(AlbumErrorCode.ALBUM_NOT_FOUND));

            // when & then
            ResultActions perform = mockMvc.perform(get("/albums/1/subscriptions"));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("ALBUM_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("앨범이 존재하지 않습니다."));
        }

        @Test
        void 앨범_참가자가_아닌_경우_예외가_발생한다() throws Exception {
            // given
            given(subscriptionService.getSubscriptionInfo(1L))
                    .willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT));

            // when & then
            ResultActions perform = mockMvc.perform(get("/albums/1/subscriptions"));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_ALBUM_PARTICIPANT"))
                    .andExpect(jsonPath("$.data.message").value("앨범에 속하지 않은 사용자입니다."));
        }

        @Test
        void 앨범_방장이_아닌_경우_예외가_발생한다() throws Exception {
            // given
            given(subscriptionService.getSubscriptionInfo(1L))
                    .willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_HOST));

            // when & then
            ResultActions perform = mockMvc.perform(get("/albums/1/subscriptions"));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_ALBUM_HOST"))
                    .andExpect(jsonPath("$.data.message").value("방장이 아닌 경우 권한이 없습니다."));
        }

        @Test
        void BASIC_유형인_경우_예외가_발생한다() throws Exception {
            // given
            given(subscriptionService.getSubscriptionInfo(1L))
                    .willThrow(
                            new CustomException(
                                    SubscriptionErrorCode
                                            .SUBSCRIPTION_NOT_SUPPORTED_FOR_BASIC_TYPE));

            // when & then
            ResultActions perform = mockMvc.perform(get("/albums/1/subscriptions"));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(
                            jsonPath("$.data.code")
                                    .value("SUBSCRIPTION_NOT_SUPPORTED_FOR_BASIC_TYPE"))
                    .andExpect(jsonPath("$.data.message").value("BASIC 유형은 구독 기능을 지원하지 않습니다."));
        }

        @Test
        void 구독이_존재하지_않는_경우_예외가_발생한다() throws Exception {
            // given
            given(subscriptionService.getSubscriptionInfo(1L))
                    .willThrow(new CustomException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

            // when & then
            ResultActions perform = mockMvc.perform(get("/albums/1/subscriptions"));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("SUBSCRIPTION_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("구독 정보가 존재하지 않습니다."));
        }
    }
}
