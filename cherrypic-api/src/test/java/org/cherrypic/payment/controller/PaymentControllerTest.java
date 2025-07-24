package org.cherrypic.payment.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cherrypic.album.enums.AlbumPlan;
import org.cherrypic.domain.payment.controller.PaymentController;
import org.cherrypic.domain.payment.dto.request.PaymentReadyRequest;
import org.cherrypic.domain.payment.dto.response.PaymentReadyResponse;
import org.cherrypic.domain.payment.exception.PaymentErrorCode;
import org.cherrypic.domain.payment.exception.PaymentException;
import org.cherrypic.domain.payment.service.PaymentService;
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

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private PaymentService paymentService;

    @Nested
    class 앨범_유료_플랜_결제_준비_요청_시 {

        @Test
        void 유효한_요청이면_결제_준비_정보를_반환한다() throws Exception {
            // given
            PaymentReadyRequest request = new PaymentReadyRequest(AlbumPlan.PRO);

            PaymentReadyResponse response =
                    new PaymentReadyResponse(
                            AlbumPlan.PRO, 3900, "album_20250723_pro_1_a5c5dd8beaa6", "상냥한 너구리");

            given(paymentService.preparePayment(request)).willReturn(response);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/payments/ready")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.plan").value("PRO"))
                    .andExpect(jsonPath("$.data.price").value("3900"))
                    .andExpect(
                            jsonPath("$.data.merchantUid")
                                    .value("album_20250723_pro_1_a5c5dd8beaa6"))
                    .andExpect(jsonPath("$.data.buyerName").value("상냥한 너구리"));
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        @ValueSource(strings = {" ", "PROO", "PREMIUMM"})
        void 앨범_플랜이_null_또는_지원하지_않는_형식이면_예외가_발생한다(String plan) throws Exception {
            // given
            PaymentReadyRequest request = new PaymentReadyRequest(AlbumPlan.from(plan));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/payments/ready")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(
                            jsonPath("$.data.message")
                                    .value("앨범 구독 플랜은 비워둘 수 없으며, PRO, PREMIUM만 지원됩니다."));
        }

        @Test
        void 앨범_플랜이_BASIC이면_예외가_발생한다() throws Exception {
            // given
            PaymentReadyRequest request = new PaymentReadyRequest(AlbumPlan.BASIC);

            given(paymentService.preparePayment(request))
                    .willThrow(new PaymentException(PaymentErrorCode.UNSUPPORTED_PAYMENT_PLAN));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/payments/ready")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("UNSUPPORTED_PAYMENT_PLAN"))
                    .andExpect(
                            jsonPath("$.data.message")
                                    .value(
                                            "해당 플랜은 유료 결제가 필요하지 않습니다. PRO 또는 PREMIUM 플랜만 결제가 가능합니다."));
        }
    }
}
